package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommands;

import java.math.BigInteger;
import java.util.*;

public class STRINGType extends ParameterType.ConcreteType {

    public static final int MAX_LEN = 30; // Probably need refactor

    public static Set<String> stringPool = new HashSet<>();
    public static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static final STRINGType instance = new STRINGType();
    public static final String signature = "java.lang.String";

    public static String generateRandomString() {
        // Now when calling text, it's impossible to generate empty string!
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = random.nextInt(MAX_LEN) + 1;
        for (int i = 0; i < length; i++) {
            // generate random index number
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    public static boolean contains(String[] strArray, String str) {
        for (String str_ : strArray) {
            if (str.equals(str_)) {
                return true;
            }
        }
        return false;
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
        // DEBUG: For testing **testNotInCollection()**
        // List<String> sList = new LinkedList<>();
        // for (int i = 0; i < 10; i++) {
        // sList.add("T" + String.valueOf(i));
        // }
        // Random rand = new Random();
        // int idx = rand.nextInt(sList.size());
        // return new Parameter(STRINGType.instance, sList.get(idx));

        Parameter ret;

        // Count a possibility for fetching from the pool
        if (stringPool.isEmpty() == false) {
            Random rand = new Random();
            int choice = rand.nextInt(10);
            if (choice <= 8) {
                // 80%: it will pick from the Pool
                List<String> stringPoolList = new ArrayList<>(stringPool);
                int idx = rand.nextInt(stringPoolList.size());
                ret = new Parameter(STRINGType.instance,
                        stringPoolList.get(idx));
                return ret;
            }
        }
        ret = new Parameter(STRINGType.instance, generateRandomString());
        if (CassandraCommands.DEBUG) {
        }
        while (!isValid(s, c, ret)) {
            if (CassandraCommands.DEBUG) {
            }
            ret = new Parameter(STRINGType.instance, generateRandomString());
        }
        stringPool.add((String) ret.value);
        return ret;
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        if (p == null || !(p.type instanceof STRINGType) || contains(
                CassandraCommands.reservedKeywords, (String) p.value)) // Specially
                                                                       // for
                                                                       // Cassandra
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
        Random rand = new Random();
        int choice = rand.nextInt(8);

        // Debug
        if (CassandraCommands.DEBUG) {
            // choice = 4; // Only test the mutate method
        }

        // TODO: Add another choice: related to the current string pool

        switch (choice) {
        // Temporally Disable bit level mutation
        case 0: // Regenerate
            if (CassandraCommands.DEBUG) {
                System.out.println("\t[String Mutation]: Regeneration");
            }
            regenerate(s, c, p);
            break;
        case 1: // Add a Byte
            if (CassandraCommands.DEBUG) {
                System.out.println("\t[String Mutation]: Add Byte");
            }
            addByte(p);
            break;
        case 2: // Delete a Byte
            if (CassandraCommands.DEBUG) {
                System.out.println("\t[String Mutation]: Delete Byte");
            }
            if (((String) p.value).isEmpty())
                return false;
            deleteByte(p);
            break;
        case 3:
            // Mutate a byte
            if (CassandraCommands.DEBUG) {
                System.out.println("\t[String Mutation]: Mutate Byte");
            }
            if (((String) p.value).isEmpty())
                return false;
            mutateByte(p);
            break;
        case 4:
            // Add a word (2 Bytes)
            addByte(p);
            addByte(p);
            break;
        case 5:
            // Delete a word
            if (((String) p.value).length() < 2)
                return false;
            deleteByte(p);
            deleteByte(p);
            break;
        case 6:
            // Mutate a word
            if (((String) p.value).isEmpty() || ((String) p.value).length() < 2)
                return false;
            mutateWord(p);
            break;
        case 7:
            // Regenerate
            p.value = generateRandomParameter(s, c).value;
            break;
        case 8:
            // Flip a Bit
            flipBit(p);
            break;
        case 9:
            // Add a Bit
            addBit(p);
            break;
        case 10:
            // Delete a Bit
            deleteBit(p);
            break;
        default:
            throw new IllegalStateException("Unexpected value: " + choice);
        }
        return true;
    }

    private String string2binary(String value) {
        return new BigInteger(value.getBytes()).toString(2);
    }

    private void flipBit(Parameter p) {
        // Can only be called at lower level
        // Assert only String can be flipped
        assert p.getValue() instanceof String;

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

        String mutatedValue = new String(
                new BigInteger(sb.toString(), 2).toByteArray());
        p.value = mutatedValue;
    }

    private void addBit(Parameter p) {
        String value = (String) p.value;
        String binary = string2binary(value);

        Random rand = new Random();
        int insertPos = rand.nextInt(binary.length());
        boolean insertBit = rand.nextBoolean();

        StringBuilder sb = new StringBuilder(binary);
        sb.insert(insertPos, insertBit ? '1' : '0');

        String mutatedValue = new String(
                new BigInteger(sb.toString(), 2).toByteArray());
        p.value = mutatedValue;
    }

    private void deleteBit(Parameter p) {
        String value = (String) p.value;
        String binary = string2binary(value);

        Random rand = new Random();
        int deletePos = rand.nextInt(binary.length());
        StringBuilder sb = new StringBuilder(binary);
        assert sb.length() == binary.length();
        sb.deleteCharAt(deletePos);

        String mutatedValue = new String(
                new BigInteger(sb.toString(), 2).toByteArray());
        p.value = mutatedValue;
    }

    private void addByte(Parameter p) {
        // Add a char
        String value = (String) p.value;
        StringBuilder sb = new StringBuilder(value);

        Random rand = new Random();
        int insertPos = rand.nextInt(sb.length());
        char insertChar = alphabet.charAt(rand.nextInt(alphabet.length()));
        sb.insert(insertPos, insertChar);
        p.value = sb.toString();
    }

    private void deleteByte(Parameter p) {
        String value = (String) p.value;
        StringBuilder sb = new StringBuilder(value);

        Random rand = new Random();
        int delPos = rand.nextInt(sb.length());
        sb.deleteCharAt(delPos);
        p.value = sb.toString();
    }

    private void mutateByte(Parameter p) {
        // Mutate a char
        String value = (String) p.value;
        StringBuilder sb = new StringBuilder(value);

        Random rand = new Random();
        int mutatePos = rand.nextInt(sb.length());
        char mutateChar = alphabet.charAt(rand.nextInt(alphabet.length()));
        sb.setCharAt(mutatePos, mutateChar);
        p.value = sb.toString();
    }

    private void mutateWord(Parameter p) {
        // Mutate a char
        String value = (String) p.value;
        StringBuilder sb = new StringBuilder(value);

        Random rand = new Random();
        int mutatePos = rand.nextInt(sb.length() - 1);
        char mutateChar1 = alphabet.charAt(rand.nextInt(alphabet.length()));
        char mutateChar2 = alphabet.charAt(rand.nextInt(alphabet.length()));

        sb.setCharAt(mutatePos, mutateChar1);
        sb.setCharAt(mutatePos + 1, mutateChar2);

        p.value = sb.toString();
    }

    @Override
    public String toString() {
        // TODO: Need change later if we want the exact type, and also need to
        // allow
        // User to modify this
        return "STRING";
    }

    public static void cleanPool() {
        stringPool.clear();
    }

}
