package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.math.BigInteger;
import java.util.Random;

public class STRINGType extends ParameterType.ConcreteType {
    public static final int MAX_LEN = 30; // Probably need refactor

    public static final STRINGType instance = new STRINGType();
    public static final String signature = "java.lang.String";

    public static String generateRandomString() {
        // Now when calling text, it's impossible to generate empty string!
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXY";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = random.nextInt(MAX_LEN) + 1;
        for(int i = 0; i < length; i++) {
            // generate random index number
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        if (init == null) {
            return generateRandomParameter(s, c);
        }
        assert init instanceof String;
        String initValue = (String) init;
        return new Parameter(STRINGType.instance, initValue);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        //  DEBUG: For testing **testNotInCollection()**
//        List<String> sList = new LinkedList<>();
//        for (int i = 0; i < 10; i++) {
//            sList.add("T" + String.valueOf(i));
//        }
//        Random rand = new Random();
//        int idx = rand.nextInt(sList.size());
//        return new Parameter(STRINGType.instance, sList.get(idx));
        Parameter ret = new Parameter(STRINGType.instance, generateRandomString());
        while (!isValid(s, c, ret)) {
            ret = new Parameter(STRINGType.instance, generateRandomString());
        }
        // Original Codes:
         return ret;
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        if (p == null || ! (p.type instanceof STRINGType))
            return false;
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        Parameter ret = generateRandomParameter(s, c);
        p.value = ret.value;
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        // TODO: Maybe need to call isValid() for checking
        return ((String) p.value).isEmpty();
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        /**
         * 1. Regenerate
         * 2. Flip/Add/Del Bit/Byte/Word/Dword
         * - These operations might need different probabilities
         */
        Random rand = new Random();
        int choice = rand.nextInt(2);

        // Debug
        choice = 1; // Only test the mutate method

        switch (choice) {
            case 0: // Regenerate
                regenerate(s, c, p);
                break;
            case 1:// Flip/Add/Del Bit/Byte/Word/Dword
                /**
                 * Cast into Bits and flip one of them
                 */
                flipBit(p); // Now only have the flipBit part.
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + choice);
        }
        return true;
    }

    public String string2binary(String value) {
        return  new BigInteger(value.getBytes()).toString(2);
    }


    public void flipBit(Parameter p) {

        String value = (String) p.value;
        String binary = string2binary(value);

        Random rand = new Random();
        int pos = rand.nextInt(binary.length());

        StringBuilder sb = new StringBuilder(binary);
        if (sb.charAt(pos) == '1') {
            sb.setCharAt(pos, '0');
        } else {
            sb.setCharAt(pos, '1');
        }

        String mutatedValue = new String(new BigInteger(sb.toString(), 2).toByteArray());
        p.value = mutatedValue;
    }

    @Override
    public String toString() {
        // TODO: Need change later if we want the exact type, and also need to allow
        // User to modify this
        return "STRING";
    }

}

