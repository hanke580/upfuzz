#!/usr/bin/env python3
# support hdfs-docker

import json
import optparse
import os
import socketserver
import sys
import time
import csv
import codecs
import socket

from six import StringIO

import os
from subprocess import Popen, PIPE


def get_shell_within_docker():
    return os

class TCPHandler(socketserver.BaseRequestHandler):
    """
    The request handler class for our server.
    It is instantiated once per connection to the server, and must
    override the handle() method to implement communication to the
    client.
    """

    shell = None
    origin_stdout = sys.stdout
    origin_stderr = sys.stderr

    def __init__(self, request, client_address, server):
        
        self.shell = get_shell_within_docker()
        self.request = request
        self.client_address = client_address
        self.server = server
        self.setup()
        try:
            self.handle()
        finally:
            self.finish()

    def handle(self):
        # executing using subprocess
        try:
            while True:
                self.data = self.request.recv(51200).strip()
                if not self.data:
                    print("stop current TCP")
                    break
                
                cmd = self.data.decode("ascii")
                start_time = time.time()
                hdfs_path = os.environ['HADOOP_HOME'] + "/bin/hdfs"
                cmds = []
                cmds.append(hdfs_path)
                cmds = cmds + cmd.split(" ")

                ret_out=""
                ret_err=""
                exit_code=-1
                
                try:
                    process = Popen(cmds, stdout=PIPE, stderr=PIPE)
                    process.wait()
                    ret_out, ret_err = process.communicate()
                    print("stdout of process: " + ret_out.decode("utf-8"))
                    exit_code = process.returncode
                    print("exit code = " + str(exit_code))
                except Exception as e:
                    print("exception: " + e)

                message_out = ""
                message_err = ""

                if ret_out != "":
                    message_out = ret_out.decode('utf-8');

                if ret_err != "":
                    message_err = ret_err.decode('utf-8');
                    
                end_time = time.time()
                resp = {
                    "cmd": cmd,
                    "exitValue": exit_code,
                    "timeUsage": end_time - start_time,
                    "message": message_out,#self.stdout_buffer.getvalue(),
                    "error": message_err # self.stderr_buffer.getvalue(),
                }
                # self.stdout_buffer.truncate(0)
                # self.stderr_buffer.truncate(0)
                self.request.sendall(json.dumps(resp).encode("ascii"))
        except Exception as e:
            print("exception1: " + str(e))
            
    def handle_(self):
        # execute using os
        try:
            while True:
                self.data = self.request.recv(51200).strip()
                if not self.data:
                    print("stop current TCP")
                    break
                
                cmd = self.data.decode("ascii")
                start_time = time.time()
                
                hdfs_path = os.environ['HADOOP_HOME'] + "/bin/hdfs"
                
                cmds = hdfs_path + " "
                cmds = cmds + cmd
                
                print("cmds = " + cmds)
                
                ret_out = os.popen(cmds)

                message_out = ""
                message_err = ""

                if ret_out != None:
                    for x in ret_out:
                        message_out += x
                    
                end_time = time.time()
                resp = {
                    "cmd": cmd,
                    "exitValue": 0,
                    "timeUsage": end_time - start_time,
                    "message": message_out,#self.stdout_buffer.getvalue(),
                    "error": message_err # self.stderr_buffer.getvalue(),
                }
                # self.stdout_buffer.truncate(0)
                # self.stderr_buffer.truncate(0)
                self.request.sendall(json.dumps(resp).encode("ascii"))
        except Exception as e:
            print("exception1: " + str(e))

class Tee(object):
    """
    replace the stdout but also write to a buffer
    """

    def __init__(self, io_type):
        self.io = io_type
        if self.io == "stdout":
            self.origin = sys.stdout
        elif self.io == "stderr":
            self.origin = sys.stderr
        self.buffer = StringIO()

    def __del__(self):
        if self.io == "stdout":
            sys.stdout = self.origin
        elif self.io == "stderr":
            sys.stderr = self.origin

    def write(self, data):
        self.origin.write(data)
        self.buffer.write(data)

    def flush(self):
        self.origin.flush()

    def getvalue(self):
        return self.buffer.getvalue()

    def truncate(self, index):
        return self.buffer.truncate(index)

    def isatty(self):
        return True


if __name__ == "__main__":
    port = os.getenv("HDFS_SHELL_DAEMON_PORT")
    host = os.getenv("HDFS_SHELL_HOST")
    print("port = " + port)
    # port = 18251
    # host = '127.0.0.1'
    if not host:
        print("No CQLSH_HOST set")
        exit()
    if port:
        port = int(port)
    if not isinstance(port, int):
        raise TypeError("port must be an integer")
    print("use " + host + ":" + str(port))
    while True:
        try:
            # sys.stdout = open("/tmp_out.log", "w");
            # sys.stderr = open("/tmp_err.log", "w");

            print('test')
            server = socketserver.TCPServer((host, port), TCPHandler)
            print('hdfs server created')
            server.serve_forever()
            print('hdfs server end')
            
        except socket.error as e:
            time.sleep(5)
            print(e)
        except Exception as e:
            print("exit exception: " + e)
            exit()
