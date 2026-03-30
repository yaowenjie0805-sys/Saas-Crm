package com.yao.crm.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CollectionsUtil {
    public static <T> Set<T> setOf(T... elements) {
        Set<T> set = new HashSet<>();
        for (T element : elements) {
            set.add(element);
        }
        return Collections.unmodifiableSet(set);
    }
}