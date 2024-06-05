#!/usr/bin/env python3
# support hbase-docker

import json
import optparse
import os
import socketserver
import sys
import time
import csv
import codecs
import socket
import tempfile
import struct
import fcntl
import re

from six import StringIO

import os
import subprocess
from subprocess import Popen, PIPE

MESSAGE_SIZE = 51200

output_file = open('/var/log/supervisor/hbase_daemon.log', 'a', encoding='utf-8')
output_file.write("test\n")

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
    # origin_stdout = sys.stdout
    # origin_stderr = sys.stderr

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
                length_bytes = self.request.recv(4)
                length = struct.unpack('>I', length_bytes)[0]
                data = b''
                while len(data) < length:
                    packet = self.request.recv(length - len(data))
                    data += packet
                self.data = data.strip()

                if not self.data:
                    print("stop current TCP")
                    output_file.write("stop current TCP\n")
                    break
                
                print(self.data)
                # output_file.write(self.data + "\n")
                cmd = self.data.decode("ascii")+'\n'
                output_file.write(cmd)
                output_file.flush()
                start_time = time.time()

                ret_out = ""
                ret_err = ""
                # exit_code=-1
                print('here1')

                try:
                    global process
                    global command_count

                    print('here2')
                    process.stdout.read()
                    process.stdout.flush()
                    process.stdin.write(self.data+b'\n')
                    process.stdin.flush()
                    print('here3')
                    next_shell_out = 'hbase(main):' + \
                        '{:0>3d}'.format(command_count) + ':0> '
                    print('next_out:', next_shell_out)
                    output_file.write('next_out: ' + next_shell_out + '\n')
                    output_file.flush()
                    command_count += 1

                    while True:
                        newline = process.stdout.read()
                        if newline is None or len(newline) == 0:
                            err_out = process.stderr.read()
                            if err_out is not None and len(err_out) != 0:
                                ret_err += err_out.decode("utf-8")
                                output_file.write('stderr: ' + ret_err)
                            # if seconds_count == 1:
                            #     seconds_count = 0
                            #     break
                            continue
                        newline = newline.decode("utf-8")
                        if not newline == cmd:
                            ret_out += newline
                        # print(newline, end='')
                        output_file.write('stdout: ' + newline)
                        output_file.flush()
                        err_out = process.stderr.read()
                        if err_out is not None and len(err_out) != 0:
                            ret_err += err_out.decode("utf-8")
                            output_file.write('stderr: ' + ret_err)
                        if "syntax error" in ret_out or "NameError" in ret_out or "NoMethodError" in ret_out or "ERROR:" in ret_out:
                            break
                        # if newline == "\n":
                        #     break
                        if ret_out.endswith("seconds") or ret_out.endswith("seconds\n") or ret_out.endswith("seconds\n\n") or ret_out.endswith("average load\n") or ret_out.endswith("\n\n"):
                            break
                        # seconds_count += 1
                        # if "=>" in ret_out:
                        #     break

                        # if process.poll() is not None:
                        #     break
                    print("stdout of process: " + ret_out)
                    # ret_out += process.stdout.read().decode('utf-8')
                    # newline = process.stdin.read()
                    # if newline:
                    #     ret_out += newline.decode('utf-8')
                    newline = process.stdout.read()
                    if newline:
                        try:
                            ret_out += newline.decode('utf-8')
                        except:
                            ret_out += "non-decodable character"
                    # newline = process.stdout.read()
                    # if newline:
                    #     ret_out += newline.decode('utf-8')
                    # process.stdout.read()
                    # while True:
                        
                    #     else:
                    #         break
                    # newline = process.stdout.read()
                    # if newline:
                    #     ret_out += newline.decode('utf-8')
                    # process.stdout.read()
                    ret_out = '\n'.join(ret_out.split('\n')[0:])
                    # ret_out = process.stdout.read() 
                    # ret_out, ret_err = process.communicate(input=cmd.encode(), timeout=5)
                    # exit_code = process.returncode
                    # print("exit code = " + str(exit_code))
                except Exception as e:
                    print("exception pipe: " + e)

                # message_out = ""
                # message_err = ""

                # if ret_out != "":
                    # message_out = ret_out.decode('utf-8');

                # if ret_err != "":
                    # message_err = ret_err.decode('utf-8');

                exit_code = 0 if len(ret_err) == 0 else -1
                print(exit_code)

                end_time = time.time()
                resp = {
                    "cmd": cmd,
                    "exitValue": exit_code,
                    "timeUsage": end_time - start_time,
                    "message": ret_out,  # self.stdout_buffer.getvalue(),
                    "error": ret_err  # self.stderr_buffer.getvalue(),
                }
                # self.stdout_buffer.truncate(0)
                # self.stderr_buffer.truncate(0)

                # Handle message exceeding limit problem
                msg = json.dumps(resp).encode("ascii")
                if len(msg) > MESSAGE_SIZE:
                    # Create a error resp
                    print("Message too large to send!")
                    resp = {
                        "cmd": cmd,
                        "exitValue": exit_code,
                        "timeUsage": end_time - start_time,
                        "message": "message too large to send: here's the first 10000 Bytes:\n" + ret_out[:10000] + "\n...",
                        "error": "message too large to send: here's the first 10000 Bytes:\n" + ret_err[:10000] + "\n..."
                    }
                    msg = json.dumps(resp).encode("ascii")

                length_prefix = struct.pack('!I', len(msg))
                self.request.sendall(length_prefix + msg)

        except Exception as e:
            print("exception1 pipe: " + str(e))


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
    port = os.getenv("HBASE_SHELL_DAEMON_PORT")
    host = os.getenv("HBASE_SHELL_HOST")
    print("port = " + port)
    # port = 18251
    # host = '127.0.0.1'
    if not host:
        print("No HBASE_HOST set")
        exit()
    if port:
        port = int(port)
    if not isinstance(port, int):
        raise TypeError("port must be an integer")
    print("use " + host + ":" + str(port))
    output_file.write("use " + host + ":" + str(port) + '\n')

    global process
    global command_count

    hbase_path = os.environ['HBASE_HOME'] + "/bin/hbase"
    process = Popen([hbase_path, "shell"], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    # process.stdin.close()
    # output = process.stdout.read()  # Read entire output
    # print(output.decode())

    # outputInit = subprocess.check_output([hbase_path, 'shell'])
    # print(outputInit.decode())
    
    '''
    hbase_path = os.environ['HBASE_HOME'] + "/bin"
    os.chdir(hbase_path)

    os.environ['HBASE_OPTS'] = '-XX:+UseConcMarkSweepGC'
    os.environ['JRUBY_OPTS'] = '-X+O'
    os.environ['CLASSPATH'] = '/hbase/hbase-2.4.15/lib/ruby/jruby-complete-9.2.13.0.jar:/hbase/hbase-2.4.15/conf:/usr/lib/jvm/java-8-openjdk-amd64//lib/tools.jar:/hbase/hbase-2.4.15:/hbase/hbase-2.4.15/lib/HikariCP-java7-2.4.12.jar:/hbase/hbase-2.4.15/lib/aopalliance-1.0.jar:/hbase/hbase-2.4.15/lib/apacheds-i18n-2.0.0-M15.jar:/hbase/hbase-2.4.15/lib/apacheds-kerberos-codec-2.0.0-M15.jar:/hbase/hbase-2.4.15/lib/api-asn1-api-1.0.0-M20.jar:/hbase/hbase-2.4.15/lib/api-i18n-1.0.0-M20.jar:/hbase/hbase-2.4.15/lib/api-util-1.0.0-M20.jar:/hbase/hbase-2.4.15/lib/asm-3.1.jar:/hbase/hbase-2.4.15/lib/avro-1.7.7.jar:/hbase/hbase-2.4.15/lib/byte-buddy-1.9.10.jar:/hbase/hbase-2.4.15/lib/byte-buddy-agent-1.9.10.jar:/hbase/hbase-2.4.15/lib/caffeine-2.8.1.jar:/hbase/hbase-2.4.15/lib/checker-qual-3.1.0.jar:/hbase/hbase-2.4.15/lib/commons-cli-1.2.jar:/hbase/hbase-2.4.15/lib/commons-codec-1.13.jar:/hbase/hbase-2.4.15/lib/commons-collections-3.2.2.jar:/hbase/hbase-2.4.15/lib/commons-compress-1.19.jar:/hbase/hbase-2.4.15/lib/commons-configuration-1.6.jar:/hbase/hbase-2.4.15/lib/commons-crypto-1.0.0.jar:/hbase/hbase-2.4.15/lib/commons-csv-1.0.jar:/hbase/hbase-2.4.15/lib/commons-daemon-1.0.13.jar:/hbase/hbase-2.4.15/lib/commons-digester-1.8.jar:/hbase/hbase-2.4.15/lib/commons-io-2.11.0.jar:/hbase/hbase-2.4.15/lib/commons-lang-2.6.jar:/hbase/hbase-2.4.15/lib/commons-lang3-3.9.jar:/hbase/hbase-2.4.15/lib/commons-math3-3.6.1.jar:/hbase/hbase-2.4.15/lib/commons-net-3.1.jar:/hbase/hbase-2.4.15/lib/curator-client-4.2.0.jar:/hbase/hbase-2.4.15/lib/curator-framework-4.2.0.jar:/hbase/hbase-2.4.15/lib/curator-recipes-4.2.0.jar:/hbase/hbase-2.4.15/lib/disruptor-3.4.2.jar:/hbase/hbase-2.4.15/lib/ehcache-3.3.1.jar:/hbase/hbase-2.4.15/lib/error_prone_annotations-2.15.0.jar:/hbase/hbase-2.4.15/lib/fst-2.50.jar:/hbase/hbase-2.4.15/lib/geronimo-jcache_1.0_spec-1.0-alpha-1.jar:/hbase/hbase-2.4.15/lib/gson-2.2.4.jar:/hbase/hbase-2.4.15/lib/guava-11.0.2.jar:/hbase/hbase-2.4.15/lib/guice-3.0.jar:/hbase/hbase-2.4.15/lib/guice-servlet-3.0.jar:/hbase/hbase-2.4.15/lib/hadoop-annotations-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-auth-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-client-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-common-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-distcp-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-hdfs-2.10.0-tests.jar:/hbase/hbase-2.4.15/lib/hadoop-hdfs-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-hdfs-client-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-mapreduce-client-app-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-mapreduce-client-common-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-mapreduce-client-core-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-mapreduce-client-hs-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-mapreduce-client-jobclient-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-mapreduce-client-shuffle-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-minicluster-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-api-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-client-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-common-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-registry-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-server-applicationhistoryservice-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-server-common-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-server-nodemanager-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-server-resourcemanager-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-server-tests-2.10.0-tests.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-server-timelineservice-2.10.0.jar:/hbase/hbase-2.4.15/lib/hadoop-yarn-server-web-proxy-2.10.0.jar:/hbase/hbase-2.4.15/lib/hbase-annotations-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-annotations-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-asyncfs-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-asyncfs-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-client-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-common-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-common-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-endpoint-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-examples-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-external-blockcache-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-hadoop-compat-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-hadoop-compat-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-hadoop2-compat-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-hadoop2-compat-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-hbtop-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-http-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-it-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-it-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-logging-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-mapreduce-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-mapreduce-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-metrics-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-metrics-api-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-procedure-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-procedure-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-protocol-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-protocol-shaded-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-replication-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-resource-bundle-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-rest-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-rsgroup-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-rsgroup-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-server-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-server-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-shaded-gson-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-shaded-jackson-jaxrs-json-provider-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-shaded-jersey-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-shaded-jetty-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-shaded-miscellaneous-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-shaded-netty-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-shaded-protobuf-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-shell-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-testing-util-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-thrift-2.4.15.jar:/hbase/hbase-2.4.15/lib/hbase-unsafe-4.1.2.jar:/hbase/hbase-2.4.15/lib/hbase-zookeeper-2.4.15-tests.jar:/hbase/hbase-2.4.15/lib/hbase-zookeeper-2.4.15.jar:/hbase/hbase-2.4.15/lib/httpclient-4.5.13.jar:/hbase/hbase-2.4.15/lib/httpcore-4.4.13.jar:/hbase/hbase-2.4.15/lib/jackson-annotations-2.13.4.jar:/hbase/hbase-2.4.15/lib/jackson-core-2.13.4.jar:/hbase/hbase-2.4.15/lib/jackson-core-asl-1.9.13.jar:/hbase/hbase-2.4.15/lib/jackson-databind-2.13.4.jar:/hbase/hbase-2.4.15/lib/jackson-mapper-asl-1.9.13.jar:/hbase/hbase-2.4.15/lib/jackson-module-jaxb-annotations-2.13.4.jar:/hbase/hbase-2.4.15/lib/jakarta.inject-2.6.1.jar:/hbase/hbase-2.4.15/lib/jakarta.validation-api-2.0.2.jar:/hbase/hbase-2.4.15/lib/jamon-runtime-2.4.1.jar:/hbase/hbase-2.4.15/lib/java-util-1.9.0.jar:/hbase/hbase-2.4.15/lib/java-xmlbuilder-0.4.jar:/hbase/hbase-2.4.15/lib/javassist-3.25.0-GA.jar:/hbase/hbase-2.4.15/lib/javax.activation-api-1.2.0.jar:/hbase/hbase-2.4.15/lib/javax.annotation-api-1.2.jar:/hbase/hbase-2.4.15/lib/javax.el-3.0.1-b08.jar:/hbase/hbase-2.4.15/lib/javax.servlet-api-3.1.0.jar:/hbase/hbase-2.4.15/lib/javax.servlet.jsp-2.3.2.jar:/hbase/hbase-2.4.15/lib/javax.servlet.jsp-api-2.3.1.jar:/hbase/hbase-2.4.15/lib/javax.ws.rs-api-2.1.1.jar:/hbase/hbase-2.4.15/lib/jaxb-api-2.3.1.jar:/hbase/hbase-2.4.15/lib/jaxb-impl-2.2.3-1.jar:/hbase/hbase-2.4.15/lib/jcip-annotations-1.0-1.jar:/hbase/hbase-2.4.15/lib/jcl-over-slf4j-1.7.33.jar:/hbase/hbase-2.4.15/lib/jcodings-1.0.55.jar:/hbase/hbase-2.4.15/lib/jets3t-0.9.0.jar:/hbase/hbase-2.4.15/lib/jettison-1.5.1.jar:/hbase/hbase-2.4.15/lib/jetty-6.1.26.jar:/hbase/hbase-2.4.15/lib/jetty-sslengine-6.1.26.jar:/hbase/hbase-2.4.15/lib/jetty-util-6.1.26.jar:/hbase/hbase-2.4.15/lib/joni-2.1.31.jar:/hbase/hbase-2.4.15/lib/jsch-0.1.54.jar:/hbase/hbase-2.4.15/lib/json-io-2.5.1.jar:/hbase/hbase-2.4.15/lib/jsr311-api-1.1.1.jar:/hbase/hbase-2.4.15/lib/jul-to-slf4j-1.7.33.jar:/hbase/hbase-2.4.15/lib/leveldbjni-all-1.8.jar:/hbase/hbase-2.4.15/lib/libthrift-0.14.1.jar:/hbase/hbase-2.4.15/lib/metrics-core-3.0.1.jar:/hbase/hbase-2.4.15/lib/metrics-core-3.2.6.jar:/hbase/hbase-2.4.15/lib/mssql-jdbc-6.2.1.jre7.jar:/hbase/hbase-2.4.15/lib/netty-buffer-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/netty-codec-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/netty-common-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/netty-handler-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/netty-resolver-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/netty-transport-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/netty-transport-native-epoll-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/netty-transport-native-unix-common-4.1.45.Final.jar:/hbase/hbase-2.4.15/lib/nimbus-jose-jwt-4.41.1.jar:/hbase/hbase-2.4.15/lib/objenesis-2.6.jar:/hbase/hbase-2.4.15/lib/okhttp-2.7.5.jar:/hbase/hbase-2.4.15/lib/okio-1.6.0.jar:/hbase/hbase-2.4.15/lib/paranamer-2.3.jar:/hbase/hbase-2.4.15/lib/protobuf-java-2.5.0.jar:/hbase/hbase-2.4.15/lib/snappy-java-1.0.5.jar:/hbase/hbase-2.4.15/lib/spymemcached-2.12.2.jar:/hbase/hbase-2.4.15/lib/stax2-api-3.1.4.jar:/hbase/hbase-2.4.15/lib/woodstox-core-5.0.3.jar:/hbase/hbase-2.4.15/lib/xmlenc-0.52.jar:/hbase/hbase-2.4.15/lib/zookeeper-3.5.7.jar:/hbase/hbase-2.4.15/lib/zookeeper-jute-3.5.7.jar:/hbase/hbase-2.4.15/lib/client-facing-thirdparty/audience-annotations-0.5.0.jar:/hbase/hbase-2.4.15/lib/client-facing-thirdparty/commons-logging-1.2.jar:/hbase/hbase-2.4.15/lib/client-facing-thirdparty/htrace-core4-4.2.0-incubating.jar:/hbase/hbase-2.4.15/lib/client-facing-thirdparty/reload4j-1.2.22.jar:/hbase/hbase-2.4.15/lib/client-facing-thirdparty/slf4j-api-1.7.33.jar:/hbase/hbase-2.4.15/lib/client-facing-thirdparty/slf4j-reload4j-1.7.33.jar'

    java_path = os.environ['JAVA_HOME'] + '/bin/java'

    run_cmds = [
        java_path,
        '-Dproc_shell',
        '-XX:OnOutOfMemoryError="kill -9 %p"',
        '-XX:+UseConcMarkSweepGC',
        '-Djava.util.logging.config.class=org.apache.hadoop.hbase.logging.JulToSlf4jInitializer',
        '-Dhbase.log.dir=/hbase/hbase-2.4.15/logs',
        '-Dhbase.log.file=hbase.log',
        '-Dhbase.home.dir=/hbase/hbase-2.4.15',
        '-Dhbase.id.str=',
        '-Dhbase.root.logger=INFO,console',
        '-Dhbase.security.logger=INFO,NullAppender',
        'org.jruby.JarBootstrapMain',
    ]

    # process = Popen(["hbase", 'shell'], stdout=PIPE, stdin=PIPE, stderr=PIPE)
    # process = Popen(run_cmds, stdout=PIPE, stdin=PIPE, stderr=PIPE)
    # output = process.communicate(input=b'version\n')[0]
    # print(output)
    '''

    os.set_blocking(process.stdout.fileno(), False)
    os.set_blocking(process.stderr.fileno(), False)
    os.set_blocking(process.stdin.fileno(), False)

    command_count = 1

    # Send an empty command (newline) to the HBase shell
    # process.stdin.write(b"list_namespace\n")
    # process.stdin.flush()

    # command_count += 1
    next_shell_out = 'hbase(main):' + '{:0>3d}'.format(command_count) + ':0> '
    exec_end = ' seconds'
    exec_end_2 = ' seconds\n'

    output = ''
    errout = ''
    buffer = b""
    
    while True:
        newline = process.stdout.read()
        # newline2 = process.stderr.readline()
        # char = process.stdout.read(1)
        
        # if not char:
        #     continue
        # buffer += char
        # if char == b"\n" or char == b">":  # Check for newline or '>'
        #     print("Buffer content:", buffer.decode().strip())  # Print or process the buffer
        #     buffer = b""  # Clear the buffer after flushing
            
        if newline is None or len(newline) == 0:
            continue
        
        # print(newline)
        newline = newline.decode("utf-8")
        # print(newline)
        
        # process.stdout.flush()
        output += newline
        err_out = process.stderr.read()
        print(output)
        if err_out is not None and len(err_out) != 0:
            errout += err_out.decode("utf-8")
            # errout += err_out

        # if output.endswith(next_shell_out):
        #     break
        if newline == "\n":
            break
        # if output.endswith(exec_end) or output.endswith(exec_end_2):
        #     break
            
    # with Popen([hbase_path, 'shell'], stdout=PIPE, stdin=PIPE, stderr=PIPE, universal_newlines=True) as p:
    #     for line in p.stdout:
    #         print(line, end='') # process line here
    #         sys.stdout.flush()
    #         if (line == next_shell_out):
    #             print("GOTCHAAAA")
    # Open a file to write the output

    # output_file_path = "shell_output.txt"
    # with open(output_file_path, "w") as output_file:
    #     # Start the subprocess with stdout redirected to the file
    #     with subprocess.Popen(['hbase/hbase-2.3.7/bin/hbase shell'], stdout=output_file, universal_newlines=True, shell=True) as p:
    #         p.wait()  # Wait for the subprocess to finish
    
    # os.system(hbase_path + ' shell > ' + output_file_path)

    # Read the output file and print its contents
    # with open(output_file, "r") as file:
    #     print(file.read())
    # process._stdin_write(b'version\n')
    # process.stdin.write(b'version\n')
    # process.stdin.write(b'version\n')
    # process.stdin.write(b'version\n')
    print("outside while loop")
    print(output)
    output_file.write(output + '\n')
    print(errout)
    output_file.write(errout + '\n')
    command_count += 1
    # Get the file descriptor of the stdin PIPE
    # stdin_fd = process.stdin.fileno()
    
    # Create a pipe
    # read_end, write_end = os.pipe()

    # Set the write end of the pipe as the stdin of the process
    # os.dup2(write_end, process.stdin.fileno())

    # fcntl.fcntl(write_end, fcntl.F_SETFL, os.O_NONBLOCK)

    # os.write(write_end, b"Input data to be sent to the process\n")

    # Set the stdin PIPE to non-blocking mode
    # os.set_blocking(stdin_fd, False)

    while True:
        try:
            # sys.stdout = open("/tmp_out.log", "w");
            # sys.stderr = open("/tmp_err.log", "w");

            print('test')
            server = socketserver.TCPServer((host, port), TCPHandler)
            print('hbase server created')
            output_file.write('hbase server created\n')
            server.serve_forever()
            print('hbase server end')

        except socket.error as e:
            time.sleep(5)
            print(e)
        except Exception as e:
            print("exit exception: " + e)
            exit()


    # hbase_path = os.environ['HBASE_HOME'] + "/bin/hbase"
    # output_file_path = "shell_output.txt"

    # # Open the output file for writing
    # with open(output_file_path, "w") as output_file:
    #     process = Popen([hbase_path, 'shell'], stdout=output_file, stdin=PIPE, stderr=output_file, universal_newlines=True)

    #     os.set_blocking(process.stdin.fileno(), False)

    #     # Send an empty command (newline) to the HBase shell
    #     # process.stdin.write('\n')
    #     # process.stdin.flush()

    #     while True:
    #         # Check if the process has finished
    #         if process.poll() is not None:
    #             break

    #     # Close the output file
    #     output_file.close()