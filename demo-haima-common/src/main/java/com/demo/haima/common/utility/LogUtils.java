package com.demo.haima.common.utility;

/**
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class LogUtils {

    private static final String PREFIX = "=== ";
    private static final String SUFFIX = " ===";
    private static final String SEPARATOR = " | ";

    /**
     *  Privatize no-args constructor
     */
    private LogUtils() { }

    /**
     *  This method is used to get the message for logging
     *
     * @param message
     * @return
     */
    public static String getMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return StringUtils.BLANK_STRING;
        }
        return PREFIX + message + SUFFIX;
    }

    /**
     *  This method is used to get the message for logging
     *
     * @param prefix a prefix message, which can be a method name, necessary illustration, etc.
     * @param message the message to be logged
     * @return the whole message to be logged
     */
    public static String getMessage(String prefix, String message) {
        if (StringUtils.isBlank(prefix) || StringUtils.isBlank(message)) {
            return StringUtils.BLANK_STRING;
        }
        return PREFIX + prefix + SEPARATOR + message + SUFFIX;
    }
}
