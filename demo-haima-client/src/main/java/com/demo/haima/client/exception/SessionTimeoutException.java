package com.demo.haima.client.exception;

import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/13
 */
public class SessionTimeoutException extends IOException {

    public SessionTimeoutException(String msg) {
        super(msg);
    }
}
