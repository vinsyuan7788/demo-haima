package com.demo.haima.client.exception;


import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/13
 */
public class ServerFoundException extends IOException {

    public ServerFoundException(String msg) {
        super(msg);
    }
}
