package com.demo.haima.fundamental.utils.exception;

/**
 * @author Vince Yuan
 * @date 2021/11/19
 */
public class DemoException extends RuntimeException {

    public DemoException() {
        super();
    }

    public DemoException(String message) {
        super(message);
    }

    public DemoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DemoException(Throwable cause) {
        super(cause);
    }

    public DemoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
