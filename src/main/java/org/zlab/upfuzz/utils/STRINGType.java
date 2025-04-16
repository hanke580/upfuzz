package org.zlab.upfuzz.utils;

import java.math.BigInteger;
import java.util.*;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class STRINGType extends ParameterType.BasicConcreteType {
    static Logger logger = LogManager.getLogger(STRINGType.class);

    public int MAX_LEN = 1024; // FIXME: what's a appropriate value?
    public int MIN_LEN = 1;
    // lower case
    public boolean onlyLowerCase = false;
    public boolean useStringPool = true;

    public static final Set<String> stringPool = new HashSet<>();
    public static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static final STRINGType instance = new STRINGType();
    public static final String signature = "java.lang.String";

    public STRINGType(int MAX_LEN) {
        this.MAX_LEN = MAX_LEN;
        assert MAX_LEN > 1;
    }

    public STRINGType(int MAX_LEN, int MIN_LEN) {
        this.MIN_LEN = MIN_LEN;
        this.MAX_LEN = MAX_LEN;
        assert MIN_LEN > 0;
        assert MAX_LEN > 1;
        assert MAX_LEN >= MIN_LEN;
    }

    public STRINGType(int MAX_LEN, int MIN_LEN, boolean onlyLowerCase) {
        this(MAX_LEN, MIN_LEN);
        this.onlyLowerCase = onlyLowerCase;
    }

    public STRINGType(int MAX_LEN, int MIN_LEN, boolean onlyLowerCase,
            boolean useStringPool) {
        this(MAX_LEN, MIN_LEN, onlyLowerCase);
        this.useStringPool = useStringPool;
    }

    public STRINGType(boolean onlyLowerCase) {
        this.onlyLowerCase = onlyLowerCase;
    }

    public STRINGType() {
    }

    public String generateRandomStringWithMinLen() {
        // Now when calling text, it's impossible to generate empty string!
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = random.nextInt(MAX_LEN) + MIN_LEN;
        for (int i = 0; i < length; i++) {
            // generate random index number
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }
        while (sb.toString().length() < MIN_LEN) {
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }
        String ret = sb.toString();
        if (onlyLowerCase) {
            ret = ret.toLowerCase();
        }
        return ret;
    }

    public String generateRandomString() {
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
        String ret = sb.toString();
        if (onlyLowerCase) {
            ret = ret.toLowerCase();
        }
        return ret;
    }

    public static boolean contains(String[] strArray, String str) {
        // case-insensitive
        for (String str_ : strArray) {
            if (str.toLowerCase().equals(str_.toLowerCase())) {
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
        if (onlyLowerCase)
            initValue = initValue.toLowerCase();
        return new Parameter(this, initValue);
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
        // return new Parameter(this, sList.get(idx));

        Parameter ret;

        // Count a possibility for fetching from the pool
        if (useStringPool && !stringPool.isEmpty()) {
            Random rand = new Random();
            int choice = rand.nextInt(6);
            if (choice <= 2) {
                // 50%: it will pick from the Pool

                // Try 3 times see whether it can get a valid String
                List<String> stringPoolList;
                synchronized (stringPool) {
                    stringPoolList = new ArrayList<>(stringPool);
                }
                for (int i = 0; i < 3; i++) {
                    int idx = rand.nextInt(stringPoolList.size());
                    String str = stringPoolList.get(idx);
                    if (onlyLowerCase)
                        str = str.toLowerCase();
                    ret = new Parameter(this, str);
                    if (isValid(s, c, ret)) {
                        return ret;
                    }
                }
            }
        }
        if (MIN_LEN == 0) {
            ret = new Parameter(this, generateRandomString());
            while (!isValid(s, c, ret)) {
                ret = new Parameter(this, generateRandomString());
            }
        } else {
            ret = new Parameter(this, generateRandomStringWithMinLen());
            while (!isValid(s, c, ret)) {
                ret = new Parameter(this, generateRandomStringWithMinLen());
            }
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
        if (p == null || !(p.type instanceof STRINGType) ||
                contains(CassandraCommand.reservedKeywords,
                        (String) p.value)) // Specially
                                           // for
                                           // Cassandra
            return false;
        if (((String) p.value).length() > MAX_LEN)
            return false;
        if (onlyLowerCase
                && !((String) p.value).equals(((String) p.value).toLowerCase()))
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
        // 60% for regenerate, 40% for mutation
        boolean regenerate = rand.nextInt(10) < 6;
        if (regenerate) {
            regenerate(s, c, p);
            return true;
        }
        String mutatedString;
        int choice = rand.nextInt(6);
        switch (choice) {
        // Temporally Disable bit level mutation
        case 0: // Add a Byte
            if (CassandraCommand.DEBUG) {
                System.out.println("\t[String Mutation]: Add Byte");
            }
            // addByte might cause a string to be in the reserved keyword
            mutatedString = addByte((String) p.value);
            break;
        case 1: // Delete a Byte
            if (CassandraCommand.DEBUG) {
                System.out.println("\t[String Mutation]: Delete Byte");
            }
            if (((String) p.value).isEmpty())
                return false;
            mutatedString = deleteByte((String) p.value);
            break;
        case 2:
            // Mutate a byte
            if (CassandraCommand.DEBUG) {
                System.out.println("\t[String Mutation]: Mutate Byte");
            }
            if (((String) p.value).isEmpty())
                return false;
            mutatedString = mutateByte((String) p.value);
            break;
        case 3:
            // Add a word (2 Bytes)
            mutatedString = addByte((String) p.value);
            mutatedString = addByte(mutatedString);
            break;
        case 4:
            // Delete a word
            if (((String) p.value).length() < 2)
                return false;
            mutatedString = deleteByte((String) p.value);
            mutatedString = deleteByte(mutatedString);
            break;
        case 5:
            // Mutate a word
            if (((String) p.value).isEmpty() || ((String) p.value).length() < 2)
                return false;
            mutatedString = mutateWord((String) p.value);
            break;
        case 6:
            // Double size of this String
            mutatedString = (String) p.value + (String) p.value;
            break;
        default:
            throw new IllegalStateException("Unexpected value: " + choice);
        }

        if (onlyLowerCase)
            mutatedString = mutatedString.toLowerCase();

        Parameter tmp = new Parameter(this, mutatedString);
        if (isValid(s, c, tmp)) {
            p.value = tmp.value;
            return true;
        }
        return false;
    }

    private String string2binary(String value) {
        return new BigInteger(value.getBytes()).toString(2);
    }

    private String flipBit(String str) {
        // Can only be called at lower level
        // Assert only String can be flipped

        String binary = string2binary(str);

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
        return mutatedValue;
    }

    private String addBit(String str) {
        String binary = string2binary(str);

        Random rand = new Random();
        int insertPos = rand.nextInt(binary.length());
        boolean insertBit = rand.nextBoolean();

        StringBuilder sb = new StringBuilder(binary);
        sb.insert(insertPos, insertBit ? '1' : '0');

        String mutatedValue = new String(
                new BigInteger(sb.toString(), 2).toByteArray());
        return mutatedValue;
    }

    private String deleteBit(String str) {
        String binary = string2binary(str);

        Random rand = new Random();
        int deletePos = rand.nextInt(binary.length());
        StringBuilder sb = new StringBuilder(binary);
        assert sb.length() == binary.length();
        sb.deleteCharAt(deletePos);

        String mutatedValue = new String(
                new BigInteger(sb.toString(), 2).toByteArray());
        return mutatedValue;
    }

    private String addByte(String str) {
        // Add a char
        try {
            StringBuilder sb = new StringBuilder(str);

            Random rand = new Random();
            int insertPos = rand.nextInt(sb.length());
            char insertChar = alphabet.charAt(rand.nextInt(alphabet.length()));
            sb.insert(insertPos, insertChar);
            return sb.toString();
        }
        // if str was empty, rand.nextInt(sb.length()); will throw the
        // StringIndex exception
        catch (IllegalArgumentException e) {
            Random rand = new Random();
            return String
                    .valueOf(alphabet.charAt(rand.nextInt(alphabet.length())));
        }
    }

    private String deleteByte(String str) {
        StringBuilder sb = new StringBuilder(str);

        Random rand = new Random();
        int delPos = rand.nextInt(sb.length());
        sb.deleteCharAt(delPos);
        return sb.toString();
    }

    private String mutateByte(String str) {
        // Mutate a char
        StringBuilder sb = new StringBuilder(str);

        Random rand = new Random();
        int mutatePos = rand.nextInt(sb.length());
        char mutateChar = alphabet.charAt(rand.nextInt(alphabet.length()));
        sb.setCharAt(mutatePos, mutateChar);
        return sb.toString();
    }

    private String mutateWord(String str) {
        // Mutate a char

        StringBuilder sb = new StringBuilder(str);

        Random rand = new Random();
        int mutatePos = rand.nextInt(sb.length() - 1);
        char mutateChar1 = alphabet.charAt(rand.nextInt(alphabet.length()));
        char mutateChar2 = alphabet.charAt(rand.nextInt(alphabet.length()));

        sb.setCharAt(mutatePos, mutateChar1);
        sb.setCharAt(mutatePos + 1, mutateChar2);

        return sb.toString();
    }

    @Override
    public String toString() {
        // TODO: Need change later if we want the exact type, and also need to
        // allow
        // User to modify this
        return "STRING";
    }

    @Override
    public boolean addToPool(Object val) {
        // FIXME: do we only allow alphebetic characters?
        if (val instanceof String && !((String) val).contains(" ")
                && !((String) val).startsWith("-")
                && !((String) val).contains("/")) {
            stringPool.add((String) val);
            return true;
        }
        return false;
    }

    public static void clearPool() {
        stringPool.clear();
    }
}
