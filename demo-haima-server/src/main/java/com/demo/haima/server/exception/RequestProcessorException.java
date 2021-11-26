package com.demo.haima.server.exception;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class RequestProcessorException extends Exception {

    public RequestProcessorException(String msg, Throwable t) {
        super(msg, t);
    }
}
