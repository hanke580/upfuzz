package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Ls extends Fs {

    /*
     * Moves files from source to destination. This command allows multiple
     * sources as well in which case the destination needs to be a directory.
     * Moving files across file systems is not permitted.
     */
    public Ls(OzoneState state) {
        super(state.subdir);

        Parameter lsCmd = new CONSTANTSTRINGType("-ls")
                .generateRandomParameter(null, null);

        Parameter pathParameter = new OzoneDirPathType()
                .generateRandomParameter(state, null);

        // -C: Display the paths of files and directories only.
        Parameter cOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-C"), null)
                        .generateRandomParameter(null, null);

        // -d: Directories are listed as plain files.
        Parameter dOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-d"), null)
                        .generateRandomParameter(null, null);

        // -h: Format file sizes in a human-readable fashion (eg 64.0m instead
        // of 67108864).
        Parameter hOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-h"), null)
                        .generateRandomParameter(null, null);

        // -q: Print ? instead of non-printable characters.
        Parameter qOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-q"), null)
                        .generateRandomParameter(null, null);

        // -R: Recursively list subdirectories encountered.

        // -t: Sort output by modification time (most recent first).
        Parameter tOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-t"), null)
                        .generateRandomParameter(null, null);

        // -S: Sort output by file size.
        Parameter sOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-S"), null)
                        .generateRandomParameter(null, null);

        // -r: Reverse the sort order.
        Parameter rOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-r"), null)
                        .generateRandomParameter(null, null);

        // -u: Use access time rather than modification time for display and
        // sorting.
        Parameter uOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-u"), null)
                        .generateRandomParameter(null, null);

        // -e: Display the erasure coding policy of files and directories
        // only.//

        params.add(lsCmd);
        params.add(cOption);
        params.add(dOption);
        params.add(hOption);
        params.add(qOption);
        params.add(tOption);
        params.add(sOption);
        params.add(rOption);
        params.add(uOption);
        params.add(pathParameter);
    }

    @Override
    public String constructCommandString() {
        return "fs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                params.get(2) + " " +
                params.get(3) + " " +
                params.get(4) + " " +
                params.get(5) + " " +
                params.get(6) + " " +
                params.get(7) + " " +
                params.get(8) + " " +
                subdir +
                params.get(9);
    }

    @Override
    public void updateState(State state) {
    }
}
