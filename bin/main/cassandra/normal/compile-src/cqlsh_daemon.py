#!/usr/bin/env python2
# support cassandra-(x<=2)

import json
import optparse
import os
import sys
import subprocess
import time
import csv
import codecs
import SocketServer as socketserver
import socket

from six import StringIO

from cqlsh import (
    setup_cqlruleset,
    setup_cqldocs,
    init_history,
    Shell,
    read_options,
    CQL_ERRORS,
    VersionNotSupported,
)

from cqlshlib.util import get_file_encoding_bomsize, trim_if_present
from cqlshlib.formatting import (
    DEFAULT_TIMESTAMP_FORMAT,
)
import cassandra

def get_shell(options, hostname, port):
    setup_cqlruleset(options.cqlmodule)
    setup_cqldocs(options.cqlmodule)
    init_history()
    csv.field_size_limit(options.field_size_limit)

    if options.file is None:
        stdin = None
    else:
        try:
            encoding, bom_size = get_file_encoding_bomsize(options.file)
            stdin = codecs.open(options.file, 'r', encoding)
            stdin.seek(bom_size)
        except IOError, e:
            sys.exit("Can't open %r: %s" % (options.file, e))

    if options.debug:
        sys.stderr.write("Using CQL driver: %s\n" % (cassandra,))
        sys.stderr.write("Using connect timeout: %s seconds\n" % (options.connect_timeout,))
        sys.stderr.write("Using '%s' encoding\n" % (options.encoding,))

    # create timezone based on settings, environment or auto-detection
    timezone = None
    if options.timezone or 'TZ' in os.environ:
        try:
            import pytz
            if options.timezone:
                try:
                    timezone = pytz.timezone(options.timezone)
                except Exception:
                    sys.stderr.write("Warning: could not recognize timezone '%s' specified in cqlshrc\n\n" % (options.timezone))
            if 'TZ' in os.environ:
                try:
                    timezone = pytz.timezone(os.environ['TZ'])
                except Exception:
                    sys.stderr.write("Warning: could not recognize timezone '%s' from environment value TZ\n\n" % (os.environ['TZ']))
        except ImportError:
            sys.stderr.write("Warning: Timezone defined and 'pytz' module for timezone conversion not installed. Timestamps will be displayed in UTC timezone.\n\n")

    # try auto-detect timezone if tzlocal is installed
    if not timezone:
        try:
            from tzlocal import get_localzone
            timezone = get_localzone()
        except ImportError:
            # we silently ignore and fallback to UTC unless a custom timestamp format (which likely
            # does contain a TZ part) was specified
            if options.time_format != DEFAULT_TIMESTAMP_FORMAT:
                sys.stderr.write("Warning: custom timestamp format specified in cqlshrc, but local timezone could not be detected."
                                 + "Either install Python 'tzlocal' module for auto-detection or specify client timezone in your cqlshrc.\n\n")

    try:
        shell = Shell(hostname,
                      port,
                      color=options.color,
                      username=options.username,
                      password=options.password,
                      stdin=stdin,
                      tty=options.tty,
                      completekey=options.completekey,
                      browser=options.browser,
                      cqlver=options.cqlversion,
                      keyspace=options.keyspace,
                      display_timestamp_format=options.time_format,
                      display_nanotime_format=options.nanotime_format,
                      display_date_format=options.date_format,
                      display_float_precision=options.float_precision,
                      display_timezone=timezone,
                      max_trace_wait=options.max_trace_wait,
                      ssl=options.ssl,
                      single_statement=options.execute,
                      request_timeout=options.request_timeout,
                      connect_timeout=options.connect_timeout,
                      encoding=options.encoding)
    except KeyboardInterrupt:
        sys.exit('Connection aborted.')
    except CQL_ERRORS, e:
        sys.exit('Connection error: %s' % (e,))
    except VersionNotSupported, e:
        sys.exit('Unsupported CQL version: %s' % (e,))
    if options.debug:
        shell.debug = True


    return shell

class TCPHandler(object):
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
        self.stdout_buffer = Tee("stdout")
        self.stderr_buffer = Tee("stderr")
        # self.origin_stdout = sys.stdout
        # self.origin_stderr = sys.stderr
        sys.stdout = self.stdout_buffer
        sys.stderr = self.stderr_buffer
        self.shell = get_shell(*read_options(sys.argv[1:], os.environ))
        self.request = request
        self.client_address = client_address
        self.server = server
        self.setup()
        try:
            self.handle()
        finally:
            self.finish()

    def handle(self):
        # self.request is the TCP socket connected to the client
        # print("handler")
        try:
            while True:
                self.data = self.request.recv(10240).strip()
                if not self.data:
                    exit(0)

                cmd = self.data.decode("ascii")

                start_time = time.time()
                ret = self.shell.onecmd(cmd)
                end_time = time.time()
                resp = {
                    "cmd": cmd,
                    "exitValue": 0 if ret == True else 1,
                    "timeUsage": end_time - start_time,
                    "message": self.stdout_buffer.getvalue(),
                    "error": self.stderr_buffer.getvalue(),
                }
                self.stdout_buffer.truncate(0)
                self.stderr_buffer.truncate(0)
                self.request.sendall(json.dumps(resp).encode("ascii"))
        except Exception as e:
            print(e)
            exit(1)

    def setup(self):
        pass

    def finish(self):
        pass


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
    port = os.getenv("CQLSH_DAEMON_PORT")
    if port:
        port = int(port)
    if not isinstance(port, int):
        raise TypeError("port must be an integer")
    # TODO change to client and retry with timeout
    print("use port: " + str(port))
    while True:
        try:
            server = socketserver.TCPServer(("0.0.0.0", port), TCPHandler)
            server.serve_forever()
        except socket.error:
            pass
        except:
            exit()
