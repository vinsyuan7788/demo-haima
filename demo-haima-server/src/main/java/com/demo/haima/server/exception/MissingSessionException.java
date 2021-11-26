package com.demo.haima.server.exception;

import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class MissingSessionException extends IOException {

    public MissingSessionException(String msg) {
        super(msg);
    }
}
