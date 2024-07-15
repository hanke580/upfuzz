#!/usr/bin/env python3

import json
import os
import socketserver
import socket
import struct
import time
import pexpect
import re

MESSAGE_SIZE = 51200

output_file = open('/var/log/supervisor/hbase_daemon.log', 'a', encoding='utf-8')
output_file.write("test\n")

def check_shell_responsiveness():
    global hbase_shell
    hbase_shell.sendline("status")
    index = hbase_shell.expect([r'hbase\(main\):\d{3}:\d+> ', pexpect.TIMEOUT, pexpect.EOF], timeout=5)
    if index == 0:
        return True
    return False

def restart_shell():
    global hbase_shell
    global command_count

    try:
        hbase_shell.close(force=True)
    except Exception as e:
        output_file.write("Exception while closing the shell: " + str(e) + "\n")
        output_file.flush()
    
    hbase_shell = pexpect.spawn(hbase_path + " shell", encoding='utf-8')
    
    startup_ready = False
    while not startup_ready:
        try:
            index = hbase_shell.expect([r'hbase\(main\):\d{3}:\d+> ', pexpect.TIMEOUT, pexpect.EOF], timeout=30)
            if index == 0:
                startup_ready = True
            elif index == 1:
                output_file.write("Timeout waiting for shell startup\n")
            elif index == 2:
                output_file.write("EOF encountered during shell startup\n")
            initial_output = hbase_shell.before
            output_file.write("Startup output: " + initial_output + "\n")
            output_file.flush()
        except Exception as e:
            output_file.write("Exception during shell startup: " + str(e) + "\n")
            output_file.flush()
            time.sleep(5)
    
    command_count = 0

class TCPHandler(socketserver.BaseRequestHandler):
    def handle(self):
        global hbase_shell
        global command_count

        try:
            while True:
                hbase_shell.before = ""
                length_bytes = self.request.recv(4)
                if not length_bytes:
                    break
                length = struct.unpack('>I', length_bytes)[0]
                data = b''
                while len(data) < length:
                    packet = self.request.recv(length - len(data))
                    if not packet:
                        break
                    data += packet
                self.data = data.strip()

                if not self.data:
                    output_file.write("stop current TCP\n")
                    break

                cmd = self.data.decode("ascii") + '\n'
                output_file.write("Received cmd: " + cmd + "\n")
                output_file.flush()
                start_time = time.time()

                if not check_shell_responsiveness() or command_count >= 500:
                    output_file.write("Shell is unresponsive or command count exceeded, restarting shell...\n")
                    restart_shell()

                hbase_shell.before = ""
                hbase_shell.sendline(cmd)
                command_count += 1
                
                # Capture the intermediate output for debugging
                start_expect_time = time.time()
                index = hbase_shell.expect([r'hbase\(main\):\d{3}:\d+> ', pexpect.TIMEOUT, pexpect.EOF], timeout=30)
                end_expect_time = time.time()
                output_file.write(f"Expect time taken: {end_expect_time - start_expect_time} seconds\n")
                output_file.flush()

                if index == 0:
                    ret_out = hbase_shell.before
                    index = ret_out.find("hbase(main)")

                    if index != -1:
                        split_string = ret_out.split('\r\n', 2)
                        if len(split_string) > 2:
                            ret_out = split_string[2]
                        else:
                            ret_out = ret_out  # If there are fewer than 3 parts, keep the original string
                    else:
                        split_string = ret_out.split('\r\n', 1)
                        if len(split_string) > 1:
                            ret_out = split_string[1]
                        else:
                            ret_out = ret_out  # If there is no '\r\n', keep the original string

                    last_index = ret_out.rfind('\r\n')
                    if last_index != -1:
                        ret_out = ret_out[:last_index] + ret_out[last_index+2:]

                    ret_out = ret_out.rstrip()
                    output_file.write("Matched prompt: hbase(main):\\d{3}:\\d+>\n")
                elif index == 1:
                    ret_out = ''
                    output_file.write("Timeout waiting for prompt\n")
                elif index == 2:
                    ret_out = ''
                    output_file.write("EOF encountered\n")

                ret_out = ret_out if ret_out else ""
                ret_err = ""

                output_file.write("Command output: " + ret_out + "\n")
                output_file.flush()

                exit_code = 0 if not ret_err else -1

                end_time = time.time()
                resp = {
                    "cmd": cmd,
                    "exitValue": exit_code,
                    "timeUsage": end_time - start_time,
                    "message": ret_out,
                    "error": ret_err
                }
                msg = json.dumps(resp).encode("ascii")
                if len(msg) > MESSAGE_SIZE:
                    resp = {
                        "cmd": cmd,
                        "exitValue": exit_code,
                        "timeUsage": end_time - start_time,
                        "message": "message too large to send: here's the first 10000 Bytes:\n" + ret_out[:10000] + "\n...",
                        "error": "message too large to send: here's the first 10000 Bytes:\n" + ret_err[:10000] + "\n..."
                    }
                    msg = json.dumps(resp).encode("ascii")

                output_file.flush()
                self.request.sendall(struct.pack('!I', len(msg)))
                self.request.sendall(msg)

        except Exception as e:
            output_file.write("Exception in handle: " + str(e) + "\n")
            output_file.flush()
            print("Exception in handle: " + str(e))


if __name__ == "__main__":
    port = os.getenv("HBASE_SHELL_DAEMON_PORT")
    host = os.getenv("HBASE_SHELL_HOST")
    output_file.write("port = " + str(port) + "\n")
    if not host:
        output_file.write("No HBASE_HOST set\n")
        exit()
    if port:
        port = int(port)
    if not isinstance(port, int):
        raise TypeError("port must be an integer")
    output_file.write("use " + host + ":" + str(port) + "\n")

    global hbase_shell
    global command_count

    hbase_path = os.environ['HBASE_HOME'] + "/bin/hbase"
    hbase_shell = pexpect.spawn(hbase_path + " shell", encoding='utf-8')

    command_count = 1
    startup_ready = False
    while not startup_ready:
        try:
            index = hbase_shell.expect([r'hbase\(main\):\d{3}:\d+> ', pexpect.TIMEOUT, pexpect.EOF], timeout=30)
            if index == 0:
                startup_ready = True
            elif index == 1:
                output_file.write("Timeout waiting for shell startup\n")
            elif index == 2:
                output_file.write("EOF encountered during shell startup\n")
            initial_output = hbase_shell.before
            output_file.write("Startup output: " + initial_output + "\n")
            output_file.flush()
        except Exception as e:
            output_file.write("Exception during shell startup: " + str(e) + "\n")
            output_file.flush()
            time.sleep(5)

    output_file.write("Started hbase shell \n")
    output_file.flush()
    hbase_shell.before = ""
    ret_out = ""

    while True:
        try:
            server = socketserver.TCPServer((host, port), TCPHandler)
            server.serve_forever()
        except socket.error as e:
            time.sleep(5)
            output_file.write("Socket error: " + str(e) + "\n")
        except Exception as e:
            output_file.write("Exit exception: " + str(e) + "\n")
            output_file.flush()
            exit()
