package com.demo.haima.common.utility;

/**
 *  This class is used for String utility
 *
 * @author Vince Yuan
 * @date 02/02/2021
 */
public class StringUtils {

    /**
     *  A blank string
     */
    public static final String BLANK_STRING = "";
    /**
     *  A null string
     */
    public static final String NULL_STRING = "null";

    /**
     *  Privatize no-args constructor
     */
    private StringUtils() { }

    public static boolean isEmpty(String string) {
        return ObjectUtils.isNull(string) || string.isEmpty();
    }

    public static boolean isNotEmpty(String string) {
        return !isEmpty(string);
    }

    public static boolean isBlank(String string) {
        return isEmpty(string) || BLANK_STRING.equals(string.trim());
    }

    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }
}
