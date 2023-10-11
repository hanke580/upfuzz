#include <stdio.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
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
#include <string.h>
#include <dirent.h>

#define TRACE_BUFFER_SIZE (64)

// #define WAIT_FOR_COMPLETION

#define PAGE_SIZE 0x1000
#define MMAP_SIZE(x) ((x & ~(PAGE_SIZE - 1)) + PAGE_SIZE)
#define round_up(x, y) (((x) + (y) - 1) & ~((y) - 1))

static volatile int child_completed = 0;

bool prefix(const char *pre, const char *str)
{
    return strncmp(pre, str, strlen(pre)) == 0;
}

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

  uint64_t size = 0;
  void* ptr = mapfile(stream_source, &size);
  clock_t start_time_2 = clock();
  if(ptr && size){
    dump_payload(ptr, size, basename(stream_source));
  }
  else{
    hprintf("Error: File not found!\n");
  }
  hprintf("[cAgent test] transfered feedback archive (%s) from nyx to host: %"PRId64" bytes sent to hypervisor in %.5f ms\n", stream_source, size, ((double)((clock() - start_time_2)*1000) / CLOCKS_PER_SEC));
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
  clock_t start_time_2 = clock();
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

  if (strstr(input_file, "stackedTestPackets")) {
    hprintf("[cAgent test] transferring test packet (%s) from host to nyx: %"PRId64" bytes received from hypervisor in %.5f ms\n", total, input_file, ((double)((clock() - start_time_2)*1000) / CLOCKS_PER_SEC));
  }
  else 
    hprintf("[hget] %"PRId64" bytes received from hypervisor! (%s)\n", total, input_file);

  if(f){
    fclose(f);
    return 0;
  }
  return -1;
}

void list_files(const char *path) {
    DIR *dir;
    struct dirent *entry;

    if (!(dir = opendir(path))) {
        perror("opendir");
        return;
    }

    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_type == DT_DIR) {
            if (strcmp(entry->d_name, ".") != 0 && strcmp(entry->d_name, "..") != 0) {
                char new_path[1024];
                snprintf(new_path, sizeof(new_path), "%s/%s", path, entry->d_name);
                // hprintf("Directory: %s\n", new_path);
                list_files(new_path);
            }
        } else {
            char file_address[1024]; // Create a char array to hold the file address
            snprintf(file_address, sizeof(file_address), "%s/%s", path, entry->d_name);
            hprintf("File: %s/%s\n", path, entry->d_name);

            // if (!(strstr(file_address, "sshd-stdout") || strstr(file_address, "sshd-stderr"))) {
            // }
        }
    }
    closedir(dir);
}

static int exec_prog(const char **argv, char *archive_name)
{
    pid_t my_pid;
    int status, timeout /* unused ifdef WAIT_FOR_COMPLETION */;

    if (0 == (my_pid = fork())) {
      hprintf("[cAgent child]: Going to archive the fuzzing storage directory \n");
      if (-1 == execve(argv[0], (char **)argv , NULL)) {
        perror("child process execve failed [%m]\n");
        return -1;
      }
      int get_file_status_2 = push_to_host(archive_name);    
      if (get_file_status_2 == -1)
        abort_operation("cAgent: ERROR! Failed to transfer fuzzing_storage log file to host.");
      hprintf("cAgent: pushed the fuzzing storage logs successfully");
      child_completed = 1;
    }

    hprintf("[cAgent parent]: from the parent process \n");

// #ifdef WAIT_FOR_COMPLETION
    timeout = 1000;

    while (0 == waitpid(my_pid , &status , WNOHANG)) {
      if ( --timeout < 0 ) {
        perror("timeout");
        return -1;
      }
      sleep(1);
    }

    hprintf("%s WEXITSTATUS %d WIFEXITED %d [status %d]\n", argv[0], WEXITSTATUS(status), WIFEXITED(status), status);

    if (1 != WIFEXITED(status) || 0 != WEXITSTATUS(status)) {
      perror("%s failed, halt system");
      return -1;
    }

// #endif
    return 0;
}

int main(int argc, char **argv) {

  /* Request information on available (host) capabilites (optional) */
  host_config_t host_config;
  kAFL_hypercall(HYPERCALL_KAFL_GET_HOST_CONFIG, (uintptr_t)&host_config);
  // hprintf("[capablities] host_config.bitmap_size: 0x%" PRIx64 "\n",
  //         host_config.bitmap_size);
  // hprintf("[capablities] host_config.ijon_bitmap_size: 0x%" PRIx64 "\n",
  //         host_config.ijon_bitmap_size);
  // hprintf("[capablities] host_config.payload_buffer_size: 0x%" PRIx64 "x\n",
  //         host_config.payload_buffer_size);

  /* this is our "bitmap" that is later shared with the fuzzer (you can also
   * pass the pointer of the bitmap used by compile-time instrumentations in
   * your target) */
  uint8_t *trace_buffer = mmap(NULL, MMAP_SIZE(TRACE_BUFFER_SIZE), PROT_READ | PROT_WRITE,
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
  // hprintf("[init] payload buffer is mapped at %p\n", payload_buffer);

  /* the main fuzzing loop */
  while (1) {
    //get_from_host("docker-compose.yaml", "docker-compose.yaml");
  
    //creating pipes
    int fds_input[2];
    int fds_output[2];
    
    // agent writes into this input pipe                      
    pipe(fds_input);
    
    // agent reads output from this output pipe                        
    pipe(fds_output);                       

    pid_t p_id = fork();
    
    // operations in child process
    if(p_id == 0) {
      // as agent reads from fds_input, duplicate the stdin file descriptor to the read end of input pipe
      dup2(fds_input[0], STDIN_FILENO);

      // as agent writes to fds_output, duplicate the stdout file descriptor to the write end of output pipe
      dup2(fds_output[1], STDOUT_FILENO);

      // the other ends of the input pipe and the output pipe should remain closed
      //close(fds_input[1]); 
      //close(fds_output[0]); 

      // run the miniclient java program located at "/home/nyx/upfuzz/Miniclient.jar" 
      // char *argv[] = {"/bin/bash", "-c", "cd /home/nyx/upfuzz/build/libs/; java -jar MiniClient.jar", NULL};
      // execv("/bin/bash", argv);
      char *argv[] = {"/bin/bash", "-c", "cd /home/nyx/upfuzz; java -jar MiniClient.jar", NULL};
      execv("/bin/bash", argv);
      exit(0);
    }
    else {
        // the read end of the input pipe and the write end of the output pipe should remain closed
        //close(fds_input[0]);
        //close(fds_output[1]);

        // these specific messages are configured in the miniclient.java program
        // when the miniclient receives the packet "START_TESTING", it will execute the test packets
        char *test_start_msg = "START_TESTING\n";
        
        // when the agent receives the packet 'R', it will know that the client is ready for testing
        char ready_state_msg = 'R';
        
        /*------------------------------------------------------------------------------------*/
        //-------------WAIT FOR EACH NODE TO BE CONNECTED VIA TCP-----------------------------//
        /*------------------------------------------------------------------------------------*/
        // char output_pkt_ready = '~';
        // if (read(fds_output[0], &output_pkt_ready, 1) != 1) {
        //   abort_operation("Read operation failed");
        // }
        char output_pkt_ready[1];  // Assuming a maximum length of 100 characters (adjust as needed)
        if (read(fds_output[0], output_pkt_ready, sizeof(output_pkt_ready)) < 0) {
          abort_operation("Read operation failed");
        }
          
        //hprintf("PACKET:::%c\n", output_pkt_ready);
        // hprintf("1. got output_pkt_ready as: %s\n", output_pkt_ready);

        // if(output_pkt_ready != ready_state_msg) {
        if(!(output_pkt_ready[0]=='R')) {
          // hprintf("2. got output_pkt_ready as: %s\n", output_pkt_ready);
          if (output_pkt_ready[0] == 'F')                                       // executor might have failed to start
          {
            hprintf("[cAgent]: got signal of failure from MiniClient \n");
            kAFL_hypercall(HYPERCALL_KAFL_USER_FAST_ACQUIRE, 0);
            int get_file_status_fdback = push_to_host("/miniClientWorkdir/stackedFeedbackPacket.ser");    
            if (get_file_status_fdback == -1)
              abort_operation("ERROR! Failed to transfer feedback test file to host.");
            // hprintf("cAgent: transferred the feedback file to the host \n");
            kAFL_hypercall(HYPERCALL_KAFL_RELEASE, 0);
            exit(0);
          }
          abort_operation("Unable to startup the target system.");
          exit(1);
        }

        /* Creates a root snapshot on first execution. Also we requested the next input with this hypercall */
        kAFL_hypercall(HYPERCALL_KAFL_USER_FAST_ACQUIRE, 0);  // root snapshot <--

        // the nodes are connected via tcp, need the test packet file name
        uint32_t len = payload_buffer->size;
        char* file_name = payload_buffer -> data;

        // transfer the test packet file from the host to the client VM
        // clock_t start_time, end_time;
        // start_time = clock();
        int get_file_status = get_from_host(file_name, "/miniClientWorkdir/mainStackedTestPacket.ser");
        if (get_file_status == -1)
          abort_operation("ERROR! Failed to transfer file from host to guest."); 
        // end_time = clock();
        // hprintf("[cAgent test] 1b: Execution time for transferring test packet from host to nyx: %.5f seconds\n", (double)((end_time - start_time)*1000) / CLOCKS_PER_SEC);

        // First snapshot created, now need to start testing, send the command "START_TESTING\n" to the client
        int send_test_status = write(fds_input[1], test_start_msg, strlen(test_start_msg));
        if (send_test_status == -1)
          abort_operation("Sending command to start testing failed.");
        
        //Read the input pipe from java client to c agent (output pipe fds_output)
        char output_pkt[24];
        if (read(fds_output[0], output_pkt, sizeof(output_pkt)) < 0)
          abort_operation("Read operation failed");
        
        if (output_pkt[0] != '2')
        {
          abort_operation("Feedback packets could not be generated.");
        }
        //hprintf("WE GOT THIS FOR 2 ::: %c\n",output_pkt );

        // sleep(120);

        // list_files("/miniClientWorkdir/");
        const char *separator = strrchr(output_pkt, ':');
        char archive_name[16];
        strncpy(archive_name, separator + 1, 15);
        archive_name[15] = '\0';
        char archive_dir[36];
        snprintf(archive_dir, sizeof(archive_dir), "/miniClientWorkdir/%s", archive_name);

        // start_time = clock();
        get_file_status = push_to_host(archive_dir);    
        if (get_file_status == -1)
          abort_operation("ERROR! Failed to transfer fuzzing storage archive to host.");
        // end_time = clock();
        // hprintf("[cAgent test] 4a: Execution time for transferring feedback archive from nyx to host: %.5f milliseconds\n", (double)((end_time - start_time)*1000) / CLOCKS_PER_SEC);

        //Push the test feedback file from client to host
        // get_file_status = push_to_host("/miniClientWorkdir/stackedFeedbackPacket.ser");    
        // if (get_file_status == -1)
        //   abort_operation("ERROR! Failed to transfer feedback test file to host.");
        
        //hprintf("WA ABLE TO SEND BACK FEEDBACK FILE");
        //Reverting the checkpoint
        kAFL_hypercall(HYPERCALL_KAFL_RELEASE, 0);
      } 
    }
  return 0; 
}
