#include <stdio.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/mman.h>
#include <stdbool.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>
#include <libgen.h>
#include <inttypes.h>
#include "nyx.h"
#include <sys/syscall.h>

#define TRACE_BUFFER_SIZE (64)

#define PAGE_SIZE 0x1000
#define MMAP_SIZE(x) ((x & ~(PAGE_SIZE - 1)) + PAGE_SIZE)
#define round_up(x, y) (((x) + (y) - 1) & ~((y) - 1))

void *mapfile(char *fn, uint64_t *size)
{
	int fd = open(fn, O_RDONLY);
	if (fd < 0)
		return NULL;
	struct stat st;
	void *map = (void *)-1L;
	if (fstat(fd, &st) >= 0) {
		*size = (uint64_t)st.st_size;
		map = mmap(NULL, round_up(*size, sysconf(_SC_PAGESIZE)),
			   PROT_READ|PROT_WRITE,
			   MAP_PRIVATE, fd, 0);
	}
	close(fd);

  if(map){
		void* copy = malloc(*size);
		memcpy(copy, map, st.st_size);
		munmap(map, round_up(*size, sysconf(_SC_PAGESIZE)));
		return copy;
	}
  return NULL;
}

static void dump_payload(void* buffer, size_t len, const char* filename){
    static bool init = false;
    static kafl_dump_file_t file_obj = {0};

    //printf("%s -> ptr: %p size: %lx - %s\n", __func__, buffer, len, filename);

    if (!init){
        file_obj.file_name_str_ptr = (uintptr_t)filename;
        file_obj.append = 0;
        file_obj.bytes = 0;
        kAFL_hypercall(HYPERCALL_KAFL_DUMP_FILE, (uintptr_t) (&file_obj));
        init=true;
    }

    file_obj.append = 1;
    file_obj.bytes = len;
    file_obj.data_ptr = (uintptr_t)buffer;
    kAFL_hypercall(HYPERCALL_KAFL_DUMP_FILE, (uintptr_t) (&file_obj));
}

int push_to_host(char* stream_source){
  char buf[256];

  if(!is_nyx_vcpu()){
    printf("Error: NYX vCPU not found!\n");
    return 0;
  }

  // if(argc != 2){
  //   hprintf("Usage: <hpush> <file>\n");
  //   return 1;
  // }

  uint64_t size = 0;
  void* ptr = mapfile(stream_source, &size);

  if(ptr && size){
    dump_payload(ptr, size, basename(stream_source));
  }
  else{
    hprintf("Error: File not found!\n");
  }
  return 0;
}

int abort_operation(char* message){
  char* error_message = NULL;
  int ret;

  if(!is_nyx_vcpu()){
    hprintf("Error: NYX vCPU not found!\n");
    return 0;
  }

  ret = asprintf(&error_message, "USER_ABORT called: %s", message);
  if (ret != -1) {
    kAFL_hypercall(HYPERCALL_KAFL_USER_ABORT, (uintptr_t)error_message);
    return 0;
  }
  kAFL_hypercall(HYPERCALL_KAFL_USER_ABORT, (uintptr_t)"USER_ABORT called!");
  return 0;
}

int get_from_host(char* input_file, char* output_file){

  void* stream_data = mmap((void*)NULL, 0x1000, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    
  FILE* f = NULL;

  uint64_t bytes = 0;
  uint64_t total = 0;

  do{
    strcpy(stream_data, input_file);
    bytes = kAFL_hypercall(HYPERCALL_KAFL_REQ_STREAM_DATA, (uintptr_t)stream_data);

#if defined(__x86_64__)
      if(bytes == 0xFFFFFFFFFFFFFFFFUL){
#else
      if(bytes == 0xFFFFFFFFUL){
#endif
      // habort("Error: Hypervisor has rejected stream buffer (file not found)");
      abort_operation("Error: Hypervisor has rejected stream buffer (file not found)");
      break;
    }

    if(f == NULL){
      f = fopen(output_file, "w+");
    }

    fwrite(stream_data, 1, bytes, f);

    total += bytes;

  } while(bytes);

  hprintf("[hget] %"PRId64" bytes received from hypervisor! (%s)\n", total, input_file);

  if(f){
    fclose(f);
    return 0;
  }
  return -1;
}


int main(int argc, char **argv) {

  /* if you want to debug code running in Nyx, hprintf() is the way to go.
   *  Long story short -- it's just a guest-to-hypervisor printf. Hence the name
   * "hprintf"
   */
  hprintf("Agent test\n");

  /* Request information on available (host) capabilites (optional) */
  host_config_t host_config;
  kAFL_hypercall(HYPERCALL_KAFL_GET_HOST_CONFIG, (uintptr_t)&host_config);
  hprintf("[capablities] host_config.bitmap_size: 0x%" PRIx64 "\n",
          host_config.bitmap_size);
  hprintf("[capablities] host_config.ijon_bitmap_size: 0x%" PRIx64 "\n",
          host_config.ijon_bitmap_size);
  hprintf("[capablities] host_config.payload_buffer_size: 0x%" PRIx64 "x\n",
          host_config.payload_buffer_size);

  /* this is our "bitmap" that is later shared with the fuzzer (you can also
   * pass the pointer of the bitmap used by compile-time instrumentations in
   * your target) */
  uint8_t *trace_buffer =
      mmap(NULL, MMAP_SIZE(TRACE_BUFFER_SIZE), PROT_READ | PROT_WRITE,
           MAP_SHARED | MAP_ANONYMOUS, -1, 0);
  memset(trace_buffer, 0,
         TRACE_BUFFER_SIZE);  // makes sure that the bitmap buffer is already
                              // mapped into the guest's memory (alternatively
                              // you can use mlock) */

  /* Submit agent configuration */
  agent_config_t agent_config = {0};
  agent_config.agent_magic = NYX_AGENT_MAGIC;
  agent_config.agent_version = NYX_AGENT_VERSION;
  agent_config.agent_timeout_detection =
      0; /* timeout detection is implemented by the agent (currently not used)
          */
  agent_config.agent_tracing =
      1; /* set this flag to propagade that instrumentation-based fuzzing is
            availabe */
  agent_config.agent_ijon_tracing = 0; /* set this flag to propagade that IJON
                                          extension is implmented agent-wise */
  agent_config.trace_buffer_vaddr =
      (uintptr_t)trace_buffer; /* trace "bitmap" pointer - required for
                                  instrumentation-only fuzzing */
  agent_config.ijon_trace_buffer_vaddr =
      (uintptr_t)NULL;                             /* "IJON" buffer pointer */
  agent_config.agent_non_reload_mode =
      1; /* non-reload mode is supported (usually because the agent implements a
            fork-server; currently not used) */
  agent_config.coverage_bitmap_size = TRACE_BUFFER_SIZE;
  kAFL_hypercall(HYPERCALL_KAFL_SET_AGENT_CONFIG, (uintptr_t)&agent_config);

  /* Tell hypervisor the virtual address of the payload (input) buffer (call
   * mlock to ensure that this buffer stays in the guest's memory)*/
  kAFL_payload *payload_buffer =
      mmap(NULL, host_config.payload_buffer_size, PROT_READ | PROT_WRITE,
           MAP_SHARED | MAP_ANONYMOUS, -1, 0);
  mlock(payload_buffer, (size_t)host_config.payload_buffer_size);
  memset(payload_buffer, 0, host_config.payload_buffer_size);
  kAFL_hypercall(HYPERCALL_KAFL_GET_PAYLOAD, (uintptr_t)payload_buffer);
  hprintf("[init] payload buffer is mapped at %p\n", payload_buffer);

  /* the main fuzzing loop */
  while (1) {

    /* Creates a root snapshot on first execution. Also we requested the next
     * input with this hypercall */
    hprintf("BEFore USE_FAST AQUIRE for loop\n");
    for(int m=0; m < 10; m++) {
        if (payload_buffer->size > 0) {
            hprintf("BEFORE DATA=%s\n", payload_buffer->data);
        } else {
            hprintf("NO DATA!!\n");
        }
        sleep(1);
    }

    get_from_host("docker-compose.yaml", "docker-compose.yaml");
    //get_from_host("fuzz_config.txt");

    int p_id = fork();
    if(p_id == 0) {
      // int docker_compose_run = system("docker-compose up");
      int docker_compose_run = system("echo hello");
      if (docker_compose_run == -1) {
        abort_operation("ERROR! Failed to run docker-compose.");
      } 
    }
    else {
        
        kAFL_hypercall(HYPERCALL_KAFL_USER_FAST_ACQUIRE, 0);  // root snapshot <--
        hprintf("---AFTER USER FAST ACQUIRE\n");
#ifdef DEBUG
        hprintf("Size: %ld Data: %x %x %x %x\n", payload_buffer->size,
            payload_buffer->data[4], payload_buffer->data[5],
            payload_buffer->data[6], payload_buffer->data[7]);
#endif

      /*---------------------------------TODO-----------------------------------------------*/
      /*------------------------------------------------------------------------------------*/
      //-------------CONNECT TCP TO EACH NODE IN THE SYSTEM---------------------------------//
      /*------------------------------------------------------------------------------------*/
      uint32_t len = payload_buffer->size;
      char* file_name = payload_buffer -> data;

      int get_file_status = get_from_host(file_name, file_name);
      if (get_file_status == -1)
        abort_operation("ERROR! Failed to transfer file from host to guest.");
      
      /*---------------------------------TODO-----------------------------------------------*/
      /*------------------------------------------------------------------------------------*/
      //Convert the test file to whatever format to be sent off through the tcp connections//
      //----Write a code in Java to serialize the test file and call that Java program------/
      /*------------------------------------------------------------------------------------*/

      /*---------------------------------TODO-----------------------------------------------*/
      /*------------------------------------------------------------------------------------*/
      //-----------Convert the feedback into a file format feedback_[test name].txt---------//
      //----Write a code in Java to serialize the test file and call that Java program------/
      /*------------------------------------------------------------------------------------*/

      get_file_status = push_to_host("feedback_test.txt");    //not sure about the filename, should we include direct too
      if (get_file_status == -1)
        abort_operation("ERROR! Failed to transfer feedback test file to host.");

      /* set a byte to make AFL++ happy (otherwise the fuzzer might refuse to
      * start fuzzing at all) */

      // ((uint8_t *)trace_buffer)[0] = 0x1;
    } 

    uint32_t len = payload_buffer->size;

    /* set a byte to make AFL++ happy (otherwise the fuzzer might refuse to
     * start fuzzing at all) */
    ((uint8_t *)trace_buffer)[0] = 0x1;

    if (len >= 4) {
    hprintf("LENGTH ==== %d\n", len);
    hprintf("DATA=%s\n", payload_buffer->data);

    ((uint8_t *)trace_buffer)[10] = 0;
    hprintf("TMP SNAPSHOT BEFORE\n");
    kAFL_hypercall(HYPERCALL_KAFL_CREATE_TMP_SNAPSHOT, 0);
    //kAFL_hypercall(HYPERCALL_KAFL_NESTED_ACQUIRE, 0);
    hprintf("TMP SNAPSHOT AFTER\n");

    /* this hypercall is used to notify the hypervisor and the fuzzer that a
     * single fuzzing "execution" has finished. If the reload-mode is enabled,
     * we will jump back to our root snapshot. Otherwise, the hypervisor passes
     * control back to the guest once the bitmap buffer has been "processed" by
     * the fuzzer.
     */
    kAFL_hypercall(HYPERCALL_KAFL_RELEASE, 0);

    /* This shouldn't happen if you have enabled the reload mode */
    hprintf("This should never happen :)\n");
  }
}
return 0;
}
