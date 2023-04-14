package org.zlab.upfuzz.nyx;

import java.util.Scanner;

public class LibnyxInterface {
    static {
        // System.loadLibrary("libnyxJNI");
        System.load(
                System.getProperty("user.dir") + "/build/libs/libnyxJNI.so");
        // TODO revert back to loadLibrary once gradle is fixed
    }

    // Note each char is 16 bit = 64 bits total
    private long nyx_process_ptr; // 64 bit pointer stored in
                                  // string, should only ever be
                                  // accessed within c

    private String sharedir;
    private String workdir;
    private int cpuID;
    private final int inputBufferSize = 1024 * 1024;
    private final boolean inputBufferWriteProtection = true;

    public LibnyxInterface(String sharedir, String workdir, int cpuID) {
        this.sharedir = sharedir;
        this.workdir = workdir;
        this.cpuID = cpuID;
    }

    public String getSharedir() {
        return this.sharedir;
    }

    public String getWorkdir() {
        return this.workdir;
    }

    public int getInputBufferSize() {
        return this.inputBufferSize;
    }

    public void nyxNew() {
        this.nyxNew(this.sharedir, this.workdir, this.cpuID,
                this.inputBufferSize, this.inputBufferWriteProtection);
    }

    // Calls nyx_new to create the new nyx-vm instance
    // This launches a VM with your specified Snapshot starting point in the
    // sharedir
    // sets the nyx_process to be used for all other functions
    private native void nyxNew(String sharedir, String workdir, int cpu_id,
            int input_buffer_size, boolean input_buffer_write_protection);

    // Calls nyx_shutdown on the nyx-vm instance
    // This kills the VM
    public native void nyxShutdown();

    // Calls nyx_exec on the nyx-vm
    // Lets the nyx-vm start the fuzzing loop, doesnt return until after
    // checkpoint is reverted.
    public native void nyxExec();

    // Calls nyx_set_afl_input on the nyx-vm
    // Sets input to be read in the fuzzing loop
    public native void setInput(String input);

    // Test Driver
    public static void testRun() {
        LibnyxInterface l = new LibnyxInterface(
                "/home/alessandro/new_nyx_mode/ubuntu/nyx_share",
                "/tmp/tmp100",
                0);
        l.nyxNew();

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();

        l.setInput("TEST");
        l.nyxExec();
        l.nyxShutdown();
    }

}
