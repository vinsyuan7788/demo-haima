package com.demo.haima.fundamental.utility.data.network.io.wrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public interface OutputWrapper {

    /**
     * This method is used to write a value
     *
     * @param value
     */
    void writeBool(boolean value);

    /**
     * This method is used to write a value
     *
     * @param value
     */
    void writeInt(int value);

    /**
     * This method is used to write a value
     *
     * @param value
     */
    void writeLong(long value);

    /**
     * This method is used to write a value
     *
     * @param value
     */
    void writeString(String value);
}
