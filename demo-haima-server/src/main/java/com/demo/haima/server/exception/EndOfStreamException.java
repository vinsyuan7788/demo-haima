package com.demo.haima.server.exception;

import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class EndOfStreamException extends IOException {

    public EndOfStreamException(String msg) {
        super(msg);
    }

    @Override
    public String toString() {
        return "EndOfStreamException: " + getMessage();
    }
}
