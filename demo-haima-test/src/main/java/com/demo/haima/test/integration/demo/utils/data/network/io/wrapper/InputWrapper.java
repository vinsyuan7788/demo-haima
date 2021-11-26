package com.demo.haima.test.integration.demo.utils.data.network.io.wrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public interface InputWrapper {

    /**
     * This method is used to read a value
     *
     * @return
     */
    boolean readBool();

    /**
     * This method is used to read a value
     *
     * @return
     */
    int readInt();

    /**
     * This method is used to read a value
     *
     * @return
     */
    long readLong();

    /**
     * This method is used to read a value
     *
     * @return
     */
    String readString();
}
