package org.zlab.upfuzz.utils;

import java.io.Serializable;

public class Pair<U, V> implements Serializable {
    public final U left;
    public final V right;

    public Pair(U left, V right) {
        this.left = left;
        this.right = right;
    }

    @Override
    // Checks specified object is "equal to" the current object or not
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pair<?, ?> pair = (Pair<?, ?>) o;

        // call `equals()` method of the underlying objects
        if (!left.equals(pair.left)) {
            return false;
        }
        return right.equals(pair.right);
    }

    @Override
    // Computes hash code for an object to support hash tables
    public int hashCode() {
        // use hash codes of the underlying objects
        return 31 * left.hashCode() + right.hashCode();
    }

    @Override
    public String toString() {
        return "(" + left + ", " + right + ")";
    }
}