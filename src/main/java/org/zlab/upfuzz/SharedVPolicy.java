package org.zlab.upfuzz;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validity Policy
 */
public class SharedVPolicy<T extends Parameter> {

    Class<T> type = null;
    public Class<T> getType() {
        if (this.type == null)
            this.type = (Class<T>) ((ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0];
        return this.type;
    }

    public List<T> chooseExcept(Set<T> set, int n) {
        List<T> ret = new ArrayList<>();
        while (ret.size() < n) {
            T obj = (T) Parameter.map.get(getType()).constructRandom();
//            T obj = (T) T.constructRandom();
            if (set.contains(obj))
                continue;
            set.add(obj);
        }
        return ret;
    }

}
