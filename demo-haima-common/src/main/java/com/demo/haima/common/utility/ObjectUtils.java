package com.demo.haima.common.utility;

/**
 *  This class is used for Object utility
 *
 * @author Vince Yuan
 * @date 05/27/2021
 */
public class ObjectUtils {

    /**
     *  Privatize no-args constructor
     */
    private ObjectUtils() { }

    public static boolean isNull(Object object) {
        return object == null;
    }

    public static boolean isNotNull(Object object) {
        return !isNull(object);
    }
}
