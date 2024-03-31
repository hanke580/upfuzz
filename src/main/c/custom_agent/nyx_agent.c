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
#include <dirent.h>

#define TRACE_BUFFER_SIZE (64)

#define PAGE_SIZE 0x1000
#define MMAP_SIZE(x) ((x & ~(PAGE_SIZE - 1)) + PAGE_SIZE)
#define round_up(x, y) (((x) + (y) - 1) & ~((y) - 1))

void *mapfile(char *fn, uint64_t *size) {
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
	
	if(!is_nyx_vcpu()) {
		printf("Error: NYX vCPU not found!\n");
		return 0;
	}
	
	uint64_t size = 0;
	void* ptr = mapfile(stream_source, &size);
	clock_t start_time_2 = clock();
	if(ptr && size) {
		dump_payload(ptr, size, basename(stream_source));
	}
	else {
		hprintf("Error: File not found!\n");
	}
	// hprintf("[cAgent test] transfered feedback archive (%s) from nyx to host: %"PRId64" bytes sent to hypervisor in %.5f ms\n", stream_source, size, ((double)((clock() - start_time_2)*1000) / CLOCKS_PER_SEC));
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
	// sleep(10000);
	if (ret != -1) {
		kAFL_hypercall(HYPERCALL_KAFL_USER_ABORT, (uintptr_t)error_message);
		return 0;
	}
	kAFL_hypercall(HYPERCALL_KAFL_USER_ABORT, (uintptr_t)"USER_ABORT called!");
	return 0;
}

int get_from_host(char* input_file, char* output_file) {
	void* stream_data = mmap((void*)NULL, 0x1000, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
	FILE* f = NULL;

	uint64_t bytes = 0;
	uint64_t total = 0;
	clock_t start_time_2 = clock();
	do {
		strcpy(stream_data, input_file);
		bytes = kAFL_hypercall(HYPERCALL_KAFL_REQ_STREAM_DATA, (uintptr_t)stream_data);

#if defined(__x86_64__)
		if(bytes == 0xFFFFFFFFFFFFFFFFUL) {
#else
			if(bytes == 0xFFFFFFFFUL) {
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
	/* Request information on available (host) capabilites (optional) */
	host_config_t host_config;
	kAFL_hypercall(HYPERCALL_KAFL_GET_HOST_CONFIG, (uintptr_t)&host_config);
	clock_t snapshot_revert_time = clock();
	
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
	
	/* the main fuzzing loop */
	
	while (1) {
		/*creating pipes */
		int fds_input[2];
		int fds_output[2];
		
		pipe(fds_input);				// agent writes into this input pipe  
		
		pipe(fds_output);                           // agent reads output from this output pipe 
		
		pid_t p_id = fork();
		
		/* operations in child process */
		if(p_id == 0) {
			/* as agent reads from fds_input, duplicate the stdin file descriptor to the read end of input pipe */
			dup2(fds_input[0], STDIN_FILENO);
			
			/* as agent writes to fds_output, duplicate the stdout file descriptor to the write end of output pipe */
			dup2(fds_output[1], STDOUT_FILENO);
			
			/* run the miniclient java program located at "/home/nyx/upfuzz/Miniclient.jar" */
			char *argv[] = {"/bin/bash", "-c", "cd /home/nyx/upfuzz; java -jar MiniClient.jar", NULL};
			execv("/bin/bash", argv);
			exit(0);
		}
		else {
			/* these specific messages are configured in the miniclient.java program 
    			* when the miniclient receives the packet "START_TESTING", it will start executing the test packets */
			char *test_start_msg_stacked = "START_TESTING0\n";                      // for starting testing without fault injection
			char *test_start_msg_testPlan = "START_TESTING4\n";                     // for starting testing with fault injection
			
			/* when the agent receives the packet 'R', it will know that the client is ready for testing */
			char ready_state_msg = 'R';
			
			/*------------------------------------------------------------------------------------*/
			//-------------WAIT FOR EACH NODE TO BE CONNECTED VIA TCP-----------------------------//
			/*------------------------------------------------------------------------------------*/
			
			char output_pkt_ready[2];
			if (read(fds_output[0], output_pkt_ready, sizeof(output_pkt_ready)) < 0) {
				abort_operation("Read operation failed");
			}
			
			// hprintf("[cAgent]: got signal from MiniClient: %s\n", output_pkt_ready[0]);
			// sleep(10000);
			if(!(output_pkt_ready[0]=='R')) {
				if (output_pkt_ready[0] == 'F')                                       // executor might have failed to start
				{
					hprintf("[cAgent]: got signal of failure from MiniClient \n");
					kAFL_hypercall(HYPERCALL_KAFL_USER_FAST_ACQUIRE, 0);
					int get_file_status_fdback;
					if (output_pkt_ready[1] == '0') 
						get_file_status_fdback = push_to_host("/miniClientWorkdir/stackedFeedbackPacket.ser");
					else
						get_file_status_fdback = push_to_host("/miniClientWorkdir/testPlanFeedbackPacket.ser");
					
					if (get_file_status_fdback == -1)
						abort_operation("ERROR! Failed to transfer feedback test file to host.");
					kAFL_hypercall(HYPERCALL_KAFL_RELEASE, 0);
					exit(0);
				}
				
				abort_operation("Unable to startup the target system.");
				exit(1);
			}
			
			/* Executor started successfully. 
    			* Creates a root snapshot on first execution. Also we requested the next input with this hypercall */
			clock_t start_time_snap = clock();
			hprintf("[cAgent]Taking root snapshot!\n");
			kAFL_hypercall(HYPERCALL_KAFL_USER_FAST_ACQUIRE, 0);  // root snapshot <--
			// hprintf("[cAgent test] Execution time for taking root snapshot: %.5f ms\n", (double)((clock() - start_time_snap)*1000) / CLOCKS_PER_SEC);
			
			/* the nodes are connected via tcp, need the test packet file name */
			uint32_t len = payload_buffer->size;
			char* file_name = payload_buffer -> data;
			
			/* transfer the test packet file from the host to the Nyx VM */
			int get_file_status;
			int test_type;
			if (file_name[0]=='s' && file_name[1]=='t' && file_name[2]=='a')                               // file_name starts with "sta", so it is of type stacked test packet 
			{
				get_file_status = get_from_host(file_name, "/miniClientWorkdir/mainStackedTestPacket.ser");
				test_type = 0;                      		// command execution without fault injection
				if (get_file_status == -1)
					abort_operation("ERROR! Failed to transfer file from host to guest.");
			} 
			else                                                                                          // file_name does not start with "sta", it is of test plan packet type 
			{
				get_file_status = get_from_host(file_name, "/miniClientWorkdir/mainTestPlanPacket.ser");
				test_type = 4;                      		// command execution with fault injection
				if (get_file_status == -1)
					abort_operation("ERROR! Failed to transfer file from host to guest.");
			}
			
			if (test_type == 0) 					// Testing with command execution only, without fault injection  
			{
				/* First snapshot created, now need to start testing, send the command "START_TESTING\n" to the client */
				int send_test_status = write(fds_input[1], test_start_msg_stacked, strlen(test_start_msg_stacked));
				if (send_test_status == -1)
					abort_operation("Sending command to start testing failed.");
			} 
			else							// Testing with fault injection 
			{
				int send_test_status = write(fds_input[1], test_start_msg_testPlan, strlen(test_start_msg_testPlan));
				if (send_test_status == -1)
					abort_operation("Sending command to start testing failed.");
			}
			
			/* Read the input pipe from java client to c agent (output pipe fds_output) */
			char output_pkt[550];
			if (read(fds_output[0], output_pkt, sizeof(output_pkt)) < 0)
				abort_operation("Read operation failed");
			
			if (output_pkt[0] != '2'){
				abort_operation("Feedback packets could not be generated.");
			}
			
			if(strchr(output_pkt, ';') != NULL)                                     // upfuzz is running in debug mode, print debug messages from nyx too
			{
				const char *separator2 = strrchr(output_pkt, ';');
				char messages[500];
				strncpy(messages, separator2 + 1, 500);
				messages[500] = '\0';
				
				hprintf("[cAgent] %s\n",messages);
			}
			
			const char *separator = strrchr(output_pkt, ':');
			char archive_name[16];
			strncpy(archive_name, separator + 1, 15);
			archive_name[15] = '\0';
			char archive_dir[36];
			snprintf(archive_dir, sizeof(archive_dir), "/miniClientWorkdir/%s", archive_name);
			hprintf("[cAgent] archive directory: %s\n", archive_dir);
			
			/* Push the test feedback file from client to host */
			get_file_status = push_to_host(archive_dir);
			if (get_file_status == -1)
				abort_operation("ERROR! Failed to transfer fuzzing storage archive to host.");
			
			/* If you want to calculate the dirty page count of nyx vm, then you need to remove the comments from the following lines */
			
			//char dirty_command[] = "dirty=$(cat /proc/meminfo | grep Dirty); value=$(echo \"$dirty\" | awk '{print $2}'); unit=$(echo \"$dirty\" | awk '{print $3}'); pagesize=$(getconf PAGESIZE); result=$(echo \"scale=2; $value*1024 / $pagesize\" | bc); echo \"$result $unit\"";
			//FILE *fp1 = popen(dirty_command, "r");
			
			//char result1[20];  // Assuming the result is less than 256 characters
			//fgets(result1, sizeof(result1), fp1);
			
			//pclose(fp1);
			
			//hprintf("[cAgent in Nyx] Dirty page count: %s\n", result1);
			
			/* Reverting to the root checkpoint */
			kAFL_hypercall(HYPERCALL_KAFL_RELEASE, 0);
		}
	}
	return 0; 
}
