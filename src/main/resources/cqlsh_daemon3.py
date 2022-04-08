#!/usr/bin/env python3
# support cassandra-(2<x)

import json
import optparse
import os
import socketserver
import sys
import time
import csv
import codecs


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
            stdin = codecs.open(options.file, "r", encoding)
            stdin.seek(bom_size)
        except IOError as e:
            sys.exit("Can't open %r: %s" % (options.file, e))

    if options.debug:
        sys.stderr.write("Using CQL driver: %s\n" % (cassandra,))
        sys.stderr.write(
            "Using connect timeout: %s seconds\n" % (options.connect_timeout,)
        )
        sys.stderr.write("Using '%s' encoding\n" % (options.encoding,))
        sys.stderr.write("Using ssl: %s\n" % (options.ssl,))

    # create timezone based on settings, environment or auto-detection
    timezone = None
    if options.timezone or "TZ" in os.environ:
        try:
            import pytz

            if options.timezone:
                try:
                    timezone = pytz.timezone(options.timezone)
                except Exception:
                    sys.stderr.write(
                        "Warning: could not recognize timezone '%s' specified in cqlshrc\n\n"
                        % (options.timezone)
                    )
            if "TZ" in os.environ:
                try:
                    timezone = pytz.timezone(os.environ["TZ"])
                except Exception:
                    sys.stderr.write(
                        "Warning: could not recognize timezone '%s' from environment value TZ\n\n"
                        % (os.environ["TZ"])
                    )
        except ImportError:
            sys.stderr.write(
                "Warning: Timezone defined and 'pytz' module for timezone conversion not installed. Timestamps will be displayed in UTC timezone.\n\n"
            )

    # try auto-detect timezone if tzlocal is installed
    if not timezone:
        try:
            from tzlocal import get_localzone

            timezone = get_localzone()
        except ImportError:
            # we silently ignore and fallback to UTC unless a custom timestamp format (which likely
            # does contain a TZ part) was specified
            if options.time_format != DEFAULT_TIMESTAMP_FORMAT:
                sys.stderr.write(
                    "Warning: custom timestamp format specified in cqlshrc, "
                    + "but local timezone could not be detected.\n"
                    + "Either install Python 'tzlocal' module for auto-detection "
                    + "or specify client timezone in your cqlshrc.\n\n"
                )

    try:
        shell = Shell(
            hostname,
            port,
            color=options.color,
            username=options.username,
            password=options.password,
            stdin=stdin,
            tty=options.tty,
            completekey=options.completekey,
            browser=options.browser,
            protocol_version=options.protocol_version,
            cqlver=options.cqlversion,
            keyspace=options.keyspace,
            display_timestamp_format=options.time_format,
            display_nanotime_format=options.nanotime_format,
            display_date_format=options.date_format,
            display_float_precision=options.float_precision,
            display_double_precision=options.double_precision,
            display_timezone=timezone,
            max_trace_wait=options.max_trace_wait,
            ssl=options.ssl,
            single_statement=options.execute,
            request_timeout=options.request_timeout,
            connect_timeout=options.connect_timeout,
            encoding=options.encoding,
        )
    except KeyboardInterrupt:
        sys.exit("Connection aborted.")
    except CQL_ERRORS as e:
        sys.exit("Connection error: %s" % (e,))
    except VersionNotSupported as e:
        sys.exit("Unsupported CQL version: %s" % (e,))
    if options.debug:
        shell.debug = True
    if options.coverage:
        shell.coverage = True
        import signal

        def handle_sighup():
            shell.stop_coverage()
            shell.do_exit()

        signal.signal(signal.SIGHUP, handle_sighup)

    return shell


class TCPHandlere(socketserver.BaseRequestHandler):
    """
    The request handler class for our server.

    It is instantiated once per connection to the server, and must
    override the handle() method to implement communication to the
    client.
    """

    shell = None
    origin_stdout = None
    origin_stderr = None

    def __init__(self, request, client_address, server):
        self.log_stream = StringIO()
        self.origin_stdout = sys.stdout
        self.origin_stderr = sys.stderr
        sys.stdout = sys.stderr = self.log_stream
        self.shell = get_shell(*read_options(sys.argv[2:], os.environ))
        super(TCPHandlere, self).__init__(request, client_address, server)

    def handle(self):
        # self.request is the TCP socket connected to the client
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
                    "message": self.log_stream.getvalue(),
                }
                self.log_stream.truncate(0)
                self.request.sendall(json.dumps(resp).encode("ascii"))
        except BrokenPipeError as e:
            print(e)
            exit(1)


if __name__ == "__main__":
    port = __reserved_port__
    server = socketserver.TCPServer(("localhost", int(port)), TCPHandlere)
    server.serve_forever()
