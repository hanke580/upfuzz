package org.zlab.upfuzz.utils;

import java.math.BigInteger;
import java.util.*;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BIGINTType extends ParameterType.BasicConcreteType {
    static Logger logger = LogManager.getLogger(STRINGType.class);

    public int NUM_BITS = 256; // Probably need refactor

    public static Set<BigInteger> pool = new HashSet<>();
    public static final BIGINTType instance = new BIGINTType();

    public BIGINTType(int NUM_BITS) {
        this.NUM_BITS = NUM_BITS;
        assert NUM_BITS > 1;
    }

    public BIGINTType() {
        this.NUM_BITS = 48;
    }

    public BigInteger generateRandomBigInt() {
        return new BigInteger(NUM_BITS, new Random());
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        if (init == null) {
            return generateRandomParameter(s, c);
        }
        assert init instanceof BigInteger;
        BigInteger initValue = (BigInteger) init;
        return new Parameter(this, initValue);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        Parameter ret;
        // Count a possibility for fetching from the pool
        if (!pool.isEmpty()) {
            Random rand = new Random();
            int choice = rand.nextInt(5);
            if (choice <= 3) {
                // 80%: it will pick from the Pool
                List<BigInteger> bigintPoolList = new ArrayList<>(pool);

                for (int i = 0; i < 3; i++) {
                    int idx = rand.nextInt(bigintPoolList.size());
                    ret = new Parameter(this,
                            bigintPoolList.get(idx));

                    if (isValid(s, c, ret)) {
                        return ret;
                    }
                }
            }
        }
        ret = new Parameter(this, generateRandomBigInt());
        while (!isValid(s, c, ret)) {
            ret = new Parameter(this, generateRandomBigInt());
        }
        pool.add((BigInteger) ret.value);
        return ret;
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (p.value).toString();
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        Parameter ret = generateRandomParameter(s, c);
        p.value = ret.value;
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        p.regenerate(s, c);
        return true;
    }

    @Override
    public String toString() {
        return "BIGINT";
    }

    @Override
    public boolean addToPool(Object val) {
        if (val instanceof BigInteger) {
            pool.add((BigInteger) val);
            return true;
        }
        return false;
    }

    public static void clearPool() {
        pool.clear();
    }
}
