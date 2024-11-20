package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Utilities;

public class Count extends Fs {
    /**
     * bin/ozone fs -count -q -h -t ARCHIVE /dir
     */
    public Count(OzoneState state) {
        super(state.subdir);
        Parameter countCmd = new CONSTANTSTRINGType("-count")
                .generateRandomParameter(null, null);
        Parameter countOptQ = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-q"), null)
                        .generateRandomParameter(null, null);
        Parameter countOptH = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-h"), null)
                        .generateRandomParameter(null, null);
        Parameter countOptV = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-v"), null)
                        .generateRandomParameter(null, null);
        Parameter countOptX = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-x"), null)
                        .generateRandomParameter(null, null);
        Parameter countOptE = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-e"), null)
                        .generateRandomParameter(null, null);

        // [-t [storageType]]
        Parameter countStorageType = new ParameterType.OptionalType(
                ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(PAIRType.instance,
                                new CONSTANTSTRINGType("-t"),
                                new ParameterType.InCollectionType(
                                        CONSTANTSTRINGType.instance,
                                        (s, c) -> Utilities
                                                .strings2Parameters(
                                                        storageTypeOptions),
                                        null)),
                null).generateRandomParameter(null, null);
        Parameter countOptU = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-u"), null)
                        .generateRandomParameter(null, null);
        Parameter dir = new OzoneDirPathType()
                .generateRandomParameter(state, null);

        params.add(countCmd);
        params.add(countOptQ);
        params.add(countOptH);
        params.add(countOptV);
        params.add(countOptX);
        if (Config.getConf().support_count_e_opt)
            params.add(countOptE);
        params.add(countStorageType);
        params.add(countOptU);
        params.add(dir);
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("fs").append(" ");
        int i = 0;
        while (i < params.size() - 1) {
            if (!params.get(i).toString().isEmpty())
                sb.append(params.get(i)).append(" ");
            i++;
        }
        sb.append(subdir).append(params.get(i));
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
    }
}
