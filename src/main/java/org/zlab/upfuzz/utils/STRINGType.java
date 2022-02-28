package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.Random;

public class STRINGType extends ParameterType.ConcreteType {

    public static final int MAX_LEN = 30;

    public static final STRINGType instance = new STRINGType();
    public static final String signature = "java.lang.String";


    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        // TODO: generate a random string.
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = random.nextInt(MAX_LEN);
        for(int i = 0; i < length; i++) {
            // generate random index number
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }
        String randomString = sb.toString();

        return new Parameter(STRINGType.instance, randomString);
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (String) p.value;
    }

//    void mutate() {
//        /**
//         * 1. Regenerate
//         * 2. Resize
//         * 3. Flip/Add/Del bit/byte/word
//         */
//    }

}

