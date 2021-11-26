package com.demo.haima.common.shutdown;

/**
 * @author Vince Yuan
 * @date 2021/11/10
 */
public enum ExitCode {

    UNEXPECTED_ERROR(1, "the code that represents unexpected error"),
    ;
    private int code;

    private String description;

    ExitCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
