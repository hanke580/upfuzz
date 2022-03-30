package org.zlab.upfuzz.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utilities {
    public static List<Integer> permutation(int size) {
        List<Integer> indexArray = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            indexArray.add(i);
        }
        Collections.shuffle(indexArray);
        return indexArray;
    }

}
