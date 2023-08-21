package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.*;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class RmDir extends Dfs {

    /*
     * Delete files specified as args.
     * If trash is enabled, file system instead moves the deleted file to a
     * trash directory (given by FileSystem#getTrashRoot). Currently, the trash
     * feature is disabled by default. User can enable trash by setting a value
     * greater than zero for parameter fs.trash.interval (in core-site.xml). See
     * expunge about deletion of files in trash. Options: The -f option will not
     * display a diagnostic message or modify the exit status to reflect an
     * error if the file does not exist. The -R option deletes the directory and
     * any content under it recursively. The -r option is equivalent to -R. The
     * -skipTrash option will bypass trash, if enabled, and delete the specified
     * file(s) immediately. This can be useful when it is necessary to delete
     * files from an over-quota directory. The -safely option will require
     * safety confirmation before deleting directory with total number of files
     * greater than hadoop.shell.delete.limit.num.files (in core-site.xml,
     * default: 100). It can be used with -skipTrash to prevent accidental
     * deletion of large directories. Delay is expected when walking over large
     * directory recursively to count the number of files to be deleted before
     * the confirmation.*
     */
    public RmDir(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter rmcmd = new CONSTANTSTRINGType("-rm")
                .generateRandomParameter(null, null);

        // The -f option will not display a diagnostic message or modify the
        // exit status to reflect an error if the file does not exist.
        Parameter fOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-f"), null)
                        .generateRandomParameter(null, null);

        // The -R option deletes the directory and any content under it
        // recursively.
        Parameter rOption = new CONSTANTSTRINGType("-R")
                .generateRandomParameter(null, null);

        // The -safely option will require safety confirmation before deleting
        // directory with total number of files greater than
        // hadoop.shell.delete.limit.num.files (in core-site.xml, default: 100).
        // It can be used with -skipTrash to prevent accidental deletion of
        // large directories. Delay is expected when walking over large
        // directory recursively to count the number of files to be deleted
        // before the confirmation.
        Parameter saveOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-safely"), null)
                        .generateRandomParameter(null, null);

        // -d : Skip creation of temporary file with the suffix ._COPYING_.
        Parameter skipOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-skipTrash"), null)
                        .generateRandomParameter(null, null);

        Parameter dstParameter = new HDFSDirPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(rmcmd);
        params.add(fOption);
        params.add(rOption);
        params.add(saveOption);
        params.add(skipOption);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                params.get(2) + " " +
                params.get(3) + " " +
                params.get(4) + " " +
                subdir +
                params.get(5);
    }

    @Override
    public void updateState(State state) {
        ((HdfsState) state).dfs.removeDir(params.get(5).toString());
    }
}
