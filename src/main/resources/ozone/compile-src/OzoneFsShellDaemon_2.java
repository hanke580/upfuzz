/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ozone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Base64;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.shell.Command;
import org.apache.hadoop.fs.shell.CommandFactory;
import org.apache.hadoop.fs.shell.FsCommand;
import org.apache.hadoop.hdfs.tools.DFSAdmin;
import org.apache.hadoop.hdfs.tools.ECAdmin;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.tools.TableListing;
import org.apache.hadoop.tracing.TraceUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Trash;
import org.apache.hadoop.ozone.shell.OzoneShell;
import org.apache.hadoop.ozone.shell.Shell;

/** Provide command line access to a FileSystem. */
@InterfaceAudience.Private
public class OzoneFsShellDaemon extends Configured implements Tool {

    static final Logger LOG = LoggerFactory.getLogger( OzoneFsShellDaemon.class );

    private static final int MAX_LINE_WIDTH = 80;

    private FileSystem fs;
    private Trash trash;
    private Help help;
    protected CommandFactory commandFactory;

    private final String usagePrefix = "Usage: ozone fs [generic options]";

    static final String SHELL_HTRACE_PREFIX = "fs.shell.htrace.";

    /**
     * Default ctor with no configuration.  Be sure to invoke
     * {@link #setConf(Configuration)} with a valid configuration prior
     * to running commands.
     */
    public OzoneFsShellDaemon() {
        this( null );
    }

    /**
     * Construct a OzoneFsShellDaemon with the given configuration.  Commands can be
     * executed via {@link #run(String[])}
     * @param conf the hadoop configuration
     */
    public OzoneFsShellDaemon( Configuration conf ) {
        super( conf );
    }

    protected FileSystem getFS() throws IOException {
        if ( fs == null ) {
            fs = FileSystem.get( getConf() );
        }
        return fs;
    }

    protected Trash getTrash() throws IOException {
        if ( this.trash == null ) {
            this.trash = new Trash( getConf() );
        }
        return this.trash;
    }

    protected Help getHelp() throws IOException {
        if ( this.help == null ) {
            this.help = new Help();
        }
        return this.help;
    }

    protected void init() {
        getConf().setQuietMode( true );
        UserGroupInformation.setConfiguration( getConf() );
        if ( commandFactory == null ) {
            commandFactory = new CommandFactory( getConf() );
            commandFactory.addObject( new Help(), "-help" );
            commandFactory.addObject( new Usage(), "-usage" );
            registerCommands( commandFactory );
        }
    }

    protected void registerCommands( CommandFactory factory ) {
        // TODO: DFSAdmin subclasses OzoneFsShellDaemon so need to protect the
        // command registration.  This class should morph into a base class for
        // commands, and then this method can be abstract
        if ( this.getClass().equals( OzoneFsShellDaemon.class ) ) {
            factory.registerCommands( FsCommand.class );
        }
    }

    /**
     * Returns the Trash object associated with this shell.
     * @return Path to the trash
     * @throws IOException upon error
     */
    // public Path getCurrentTrashDir() throws IOException {
    //     return getTrash().getCurrentTrashDir();
    // }

    /**
     * Returns the current trash location for the path specified
     * @param path to be deleted
     * @return path to the trash
     * @throws IOException
     */
    public Path getCurrentTrashDir( Path path ) throws IOException {
        return getTrash().getCurrentTrashDir( path );
    }

    protected String getUsagePrefix() {
        return usagePrefix;
    }

    // NOTE: Usage/Help are inner classes to allow access to outer methods
    // that access commandFactory

    /**
     *  Display help for commands with their short usage and long description.
     */
    protected class Usage extends FsCommand {
        public static final String NAME = "usage";
        public static final String USAGE = "[cmd ...]";
        public static final String DESCRIPTION =
            "Displays the usage for given command or all commands if none "
            + "is specified.";

        @Override
        protected void processRawArguments( LinkedList< String > args ) {
            if ( args.isEmpty() ) {
                printUsage( System.out );
            }
            else {
                for ( String arg : args )
                    printUsage( System.out, arg );
            }
        }
    }

    /**
     * Displays short usage of commands sans the long description
     */
    protected class Help extends FsCommand {
        public static final String NAME = "help";
        public static final String USAGE = "[cmd ...]";
        public static final String DESCRIPTION =
            "Displays help for given command or all commands if none "
            + "is specified.";

        @Override
        protected void processRawArguments( LinkedList< String > args ) {
            if ( args.isEmpty() ) {
                printHelp( System.out );
            }
            else {
                for ( String arg : args )
                    printHelp( System.out, arg );
            }
        }
    }

    /*
     * The following are helper methods for getInfo().  They are defined
     * outside of the scope of the Help/Usage class because the run() method
     * needs to invoke them too.
     */

    // print all usages
    private void printUsage( PrintStream out ) {
        printInfo( out, null, false );
    }

    // print one usage
    private void printUsage( PrintStream out, String cmd ) {
        printInfo( out, cmd, false );
    }

    // print all helps
    private void printHelp( PrintStream out ) {
        printInfo( out, null, true );
    }

    // print one help
    private void printHelp( PrintStream out, String cmd ) {
        printInfo( out, cmd, true );
    }

    private void printInfo( PrintStream out, String cmd, boolean showHelp ) {
        if ( cmd != null ) {
            // display help or usage for one command
            Command instance = commandFactory.getInstance( "-" + cmd );
            if ( instance == null ) {
                throw new UnknownCommandException( cmd );
            }
            if ( showHelp ) {
                printInstanceHelp( out, instance );
            }
            else {
                printInstanceUsage( out, instance );
            }
        }
        else {
            // display help or usage for all commands
            out.println( getUsagePrefix() );

            // display list of short usages
            ArrayList< Command > instances = new ArrayList< Command >();
            for ( String name : commandFactory.getNames() ) {
                Command instance = commandFactory.getInstance( name );
                if ( !instance.isDeprecated() ) {
                    out.println( "\t[" + instance.getUsage() + "]" );
                    instances.add( instance );
                }
            }
            // display long descriptions for each command
            if ( showHelp ) {
                for ( Command instance : instances ) {
                    out.println();
                    printInstanceHelp( out, instance );
                }
            }
            out.println();
            ToolRunner.printGenericCommandUsage( out );
        }
    }

    private void printInstanceUsage( PrintStream out, Command instance ) {
        out.println( getUsagePrefix() + " " + instance.getUsage() );
    }

    private void printInstanceHelp( PrintStream out, Command instance ) {
        out.println( instance.getUsage() + " :" );
        TableListing listing = null;
        final String prefix = "  ";
        for ( String line : instance.getDescription().split( "\n" ) ) {
            if ( line.matches( "^[ \t]*[-<].*$" ) ) {
                String[] segments = line.split( ":" );
                if ( segments.length == 2 ) {
                    if ( listing == null ) {
                        listing = createOptionTableListing();
                    }
                    listing.addRow( segments[0].trim(), segments[1].trim() );
                    continue;
                }
            }

            // Normal literal description.
            if ( listing != null ) {
                for ( String listingLine : listing.toString().split( "\n" ) ) {
                    out.println( prefix + listingLine );
                }
                listing = null;
            }

            for ( String descLine :
                  StringUtils.wrap( line, MAX_LINE_WIDTH, "\n", true )
                      .split( "\n" ) ) {
                out.println( prefix + descLine );
            }
        }

        if ( listing != null ) {
            for ( String listingLine : listing.toString().split( "\n" ) ) {
                out.println( prefix + listingLine );
            }
        }
    }

    // Creates a two-row table, the first row is for the command line option,
    // the second row is for the option description.
    private TableListing createOptionTableListing() {
        return new TableListing.Builder()
            .addField( "" )
            .addField( "", true )
            .wrapWidth( MAX_LINE_WIDTH )
            .build();
    }

    /**
     * run
     */
    @Override
    public int run( String[] argv ) {
        // initialize OzoneFsShellDaemon

        ServerSocket server;
        Socket socket;
        DataInputStream in;
        DataOutputStream out;
        Configuration conf = new Configuration();
        Long st;
        DateFormat formatter =
            new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );

        init();
        // Tracer tracer = new Tracer.Builder( "OzoneFsShellDaemon" )
        //                     .conf( TraceUtils.wrapHadoopConf(
        //                         SHELL_HTRACE_PREFIX, getConf() ) )
        //                     .build();
        // Tracer tracer = new Tracer.Builder("OzoneFsShellDaemon")
        //                 .conf(new TraceConfiguration(getConf(), SHELL_HTRACE_PREFIX))
        //                 .build();
        // Tracer tracer = new Tracer.Builder("OzoneFsShellDaemon")
        //                 .conf(HTraceConfiguration.fromMap(getConf().getPropsWithPrefix(SHELL_HTRACE_PREFIX)))
        //                 .build();
        int exitCode = -1;

        try {
            server = new ServerSocket( 11116, 0,
                                       InetAddress.getByName( "0.0.0.0" ) );
            socket = server.accept();
            System.out.println("accept socket");
            in = new DataInputStream( socket.getInputStream() );
            out = new DataOutputStream( socket.getOutputStream() );
            Gson gson = new Gson();

            while( true ){
                st = System.currentTimeMillis();
                int packetLength = in.readInt();
                byte[] bytes = new byte[packetLength];

                int len = 0;
                len = in.read( bytes, len, packetLength - len );
                while ( len < packetLength ) {
                    int size = in.read( bytes, len, packetLength - len );
                    len += size;
                }
                String commandString = new String(bytes);
                // System.out.println("command: " + commandString);
                String[] fullCommands = commandString.split("\\s+");
                String cmdType = fullCommands[0];
                String[] commands =
                    Arrays.copyOfRange(fullCommands, 1, fullCommands.length);
                // for (int i = 0; i < commands.length;++i) {
                //     System.out.println("command i: " + i + " " + commands[i]);
                // }
                OzonePacket ozonePacket = new OzonePacket();

                if(cmdType.equals("dfsadmin") || cmdType.equals("ec")){
                    ozonePacket.cmd = commandString;
                    try{
                        int res;
                        if (cmdType.equals("ec")) {
                            res = ToolRunner.run(new ECAdmin(new Configuration()), commands);
                        } else {
                            res = ToolRunner.run(new DFSAdmin(), commands);
                        }
                        ozonePacket.exitValue = res;
                        ozonePacket.message = "";
                        ozonePacket.error = "";
                        ozonePacket.timeUsage = (System.currentTimeMillis() - st) / 1000.;
                    }
                    catch(Exception e){
                        ozonePacket.exitValue = -1;
                        ozonePacket.message = "";
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        ozonePacket.error = sw.toString();
                        ozonePacket.timeUsage = (System.currentTimeMillis() - st) / 1000.;
                    }
                } else if (cmdType.equals("fs")) {
                    // dfs command
                    String cmd = commands[0];
                    Command instance = null;
                    try {
                        instance = commandFactory.getInstance( cmd );
                        if ( instance == null ) {
                            throw new UnknownCommandException();
                        }
                        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                        ByteArrayOutputStream byteErrput = new ByteArrayOutputStream();
                        PrintStream cmdOutput = new PrintStream(byteOutput, true);
                        PrintStream cmdErrput = new PrintStream(byteErrput, true);
                        instance.out = cmdOutput;
                        instance.err = cmdOutput;

                        exitCode = instance.run(
                                Arrays.copyOfRange( commands, 1, commands.length ) );
                        String encodedString = Base64.getEncoder().encodeToString(byteOutput.toString().getBytes());
                        ozonePacket.cmd = commandString;
                        ozonePacket.exitValue = exitCode;
                        ozonePacket.message = encodedString;
                        ozonePacket.error = byteErrput.toString();
                        ozonePacket.timeUsage = (System.currentTimeMillis() - st) / 1000.;
                        
                    } catch ( Exception e ) {
                        ozonePacket.cmd = commandString;
                        ozonePacket.exitValue = -1;
                        ozonePacket.message = "";
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        ozonePacket.error = sw.toString();
                        ozonePacket.timeUsage = (System.currentTimeMillis() - st) / 1000.;
                    }
                } else if (cmdType.equals("sh")) {
                    ozonePacket.cmd = commandString;
                    ByteArrayOutputStream outStr = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(outStr);

                    try{
                        PrintStream oldOut = System.out;
                        PrintStream oldErr = System.err;
                        System.setOut(ps);
                        System.setErr(ps);

                        OzoneShell ozoneShell = new OzoneShell();
                        ozoneShell.execute(Arrays.copyOfRange( commands, 0, commands.length ));
                        // ByteArrayOutputStream outStr = new ByteArrayOutputStream();
                        // PrintStream ps = new PrintStream(outStr);
                        System.setOut(oldOut);
                        System.setErr(oldErr);

                        ozonePacket.exitValue = 0;
                        String encodedString = Base64.getEncoder().encodeToString(outStr.toString().getBytes());
                        ozonePacket.message = encodedString;
                        ozonePacket.error = "";
                        ozonePacket.timeUsage = (System.currentTimeMillis() - st) / 1000.;
                    } catch (Exception e) {
                        // ByteArrayOutputStream outStr = new ByteArrayOutputStream();
                        // PrintStream ps = new PrintStream(outStr);
                        e.printStackTrace(ps);
                        ozonePacket.exitValue = -1;
                        String encodedString = Base64.getEncoder().encodeToString(outStr.toString().getBytes());
                        ozonePacket.message = "";
                        ozonePacket.error = encodedString;
                        ozonePacket.timeUsage = (System.currentTimeMillis() - st) / 1000.0;
                    }
                } 
                else {
                    // command is not supported for ozone-daemon
                    ozonePacket.cmd = commandString;
                    ozonePacket.exitValue = -1;
                    ozonePacket.message = "";
                    ozonePacket.error = "Command is not supported for daemon process";
                    ozonePacket.timeUsage = (System.currentTimeMillis() - st) / 1000.;
                }
                String data = gson.toJson(ozonePacket, OzonePacket.class);
                out.writeInt(data.length());
                out.write(data.getBytes());
            }
        }
        catch ( IOException e ) {
        }

        // tracer.close();
        st = System.currentTimeMillis();
        System.out.println( "end run: " + formatter.format( st ) );
        return exitCode;
    }

    private void displayError( String cmd, String message ) {
        for ( String line : message.split( "\n" ) ) {
            System.err.println( cmd + ": " + line );
            if ( cmd.charAt( 0 ) != '-' ) {
                Command instance = null;
                instance = commandFactory.getInstance( "-" + cmd );
                if ( instance != null ) {
                    System.err.println( "Did you mean -" + cmd
                                        + "?  This command "
                                        + "begins with a dash." );
                }
            }
        }
    }

    /**
     *  Performs any necessary cleanup
     * @throws IOException upon error
     */
    public void close() throws IOException {
        if ( fs != null ) {
            fs.close();
            fs = null;
        }
    }

    /**
     * main() has some simple utility methods
     * @param argv the command and its arguments
     * @throws Exception upon error
     */
    public static void main( String argv[] ) throws Exception {
        Long st = System.currentTimeMillis();
        DateFormat formatter =
            new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );
        OzoneFsShellDaemon shell = newShellInstance();
        Configuration conf = new Configuration();
        conf.setQuietMode( false );
        shell.setConf( conf );
        int res;
        try {
            res = ToolRunner.run( shell, argv );
            Long ed = System.currentTimeMillis();
            System.out.println( "leave main: " + formatter.format( ed )
                                + "\ndiff: " + ( ed - st ) );
        }
        finally { shell.close(); }
        System.exit( res );
    }

    // TODO: this should be abstract in a base class
    protected static OzoneFsShellDaemon newShellInstance() {
        return new OzoneFsShellDaemon();
    }

    /**
     * The default ctor signals that the command being executed does not exist,
     * while other ctor signals that a specific command does not exist.  The
     * latter is used by commands that process other commands, ex. -usage/-help
     */
    @SuppressWarnings( "serial" )
    static class UnknownCommandException extends IllegalArgumentException {
        private final String cmd;
        UnknownCommandException() {
            this( null );
        }
        UnknownCommandException( String cmd ) {
            this.cmd = cmd;
        }

        @Override
        public String getMessage() {
            return ( ( cmd != null ) ? "`" + cmd + "': " : "" )
                + "Unknown command";
        }
    }

    public static class OzonePacket {
        public String cmd;
        public int exitValue;
        public String message;
        public String error;
        public double timeUsage;

        public OzonePacket() {
            cmd = "";
            exitValue = 0;
            message = "";
            error = "";
            timeUsage = -1;
        }
    }
}
