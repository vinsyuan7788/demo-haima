package com.demo.haima.common.utility;

import java.util.Collection;

/**
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class CollectionUtils<E> {

    /**
     *  Privatize no-args constructor
     */
    private CollectionUtils() { }

    public static boolean isEmpty(Collection collection) {
        return ObjectUtils.isNull(collection) || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection collection) {
        return !isEmpty(collection);
    }
}
