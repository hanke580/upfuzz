package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class STRINGType extends ParameterType.ConcreteType {
    public static final int MAX_LEN = 30;

    public static final STRINGType instance = new STRINGType();
    public static final String signature = "java.lang.String";

    public static String generateRandomString() {
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
        return sb.toString();
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        // TODO: generate a random string.

        //  For testing **testNotInCollection()**
        List<String> sList = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            sList.add("T" + String.valueOf(i));
        }

        Random rand = new Random();
        int idx = rand.nextInt(sList.size());

        return new Parameter(STRINGType.instance, sList.get(idx));

        // Original Codes:
//         return new Parameter(STRINGType.instance, generateRandomString());
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public void fixIfNotValid(State s, Command c, Parameter p) {

    }

//    void mutate() {
//        /**
//         * 1. Regenerate
//         * 2. Resize
//         * 3. Flip/Add/Del bit/byte/word
//         */
//    }

}

