package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LONGType extends ParameterType.BasicConcreteType {
    // [min, max)

    public static Set<Long> longPool = new HashSet<>();
    public final Long max;
    public final Long min;

    public static final int RETRY_POOL_TIME = 5;

    public static final String signature = "java.lang.Long";

    public LONGType() {
        max = null;
        min = null;
    }

    public LONGType(Long max) {
        this.min = null;
        this.max = max;
    }

    public LONGType(Long min, Long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        if (init == null)
            return generateRandomParameter(s, c);
        assert init instanceof Long;
        Long initValue = (Long) init;
        return new Parameter(this, initValue);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        Long value;

        if (longPool.isEmpty() == false) {
            Random rand = new Random();
            long choice = rand.nextInt(5);
            if (choice <= 3) {
                // 80%: it will pick from the Pool
                List<Long> longPoolList = new ArrayList<>(longPool);

                for (int i = 0; i < RETRY_POOL_TIME; i++) {
                    int idx = rand.nextInt(longPoolList.size());
                    value = longPoolList.get(idx);

                    if (max == null && min == null) {
                        return new Parameter(this, value);
                    } else if (max != null && min == null) {
                        if (value < max)
                            return new Parameter(this, value);
                    } else if (max == null && min != null) {
                        if (value >= min)
                            return new Parameter(this, value);
                    } else {
                        if (value < max && value >= min)
                            return new Parameter(this, value);
                    }
                }
            }
        }

        if (max == null && min == null) {
            value = new Random().nextLong();
        } else if (max != null && min == null) {
            value = ThreadLocalRandom.current().nextLong(max);
        } else if (max == null && min != null) {
            value = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE) + min;
        } else {
            value = ThreadLocalRandom.current().nextLong(max - min) + min;
        }
        longPool.add(value);
        return new Parameter(this, value);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p.type instanceof LONGType;
        return String.valueOf((long) p.value);
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        assert p.value instanceof Long;
        boolean ret;
        if (max == null && min == null) {
            ret = true;
        } else if (max != null && min == null) {
            ret = (Long) p.value < max;
        } else if (max == null && min != null) {
            ret = (Long) p.value >= min;
        } else {
            ret = (Long) p.value < max && (Long) p.value >= min;
        }
        return ret;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        if (!isValid(s, c, p)) {
            p.value = generateRandomParameter(s, c).value;
        }
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        p.value = generateRandomParameter(s, c).value;
        return true;
    }

    @Override
    public String toString() {
        return "LONG";
    }

    @Override
    public boolean addToPool(Object val) {
        if (val instanceof Long) {
            longPool.add((Long) val);
            return true;
        }
        return false;
    }

    public static void clearPool() {
        longPool.clear();
    }
}
