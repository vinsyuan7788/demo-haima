package com.demo.haima.fundamental.utility.data.network.io.stream;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/19
 */
public class ByteBufferInputStream extends InputStream {

    private ByteBuffer byteBuffer;

    public ByteBufferInputStream(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public int read() {
        if (byteBuffer.hasRemaining()) {
            // Why 0xff here: it may avoid the "EOF Exception" while reading long-typed value
            return byteBuffer.get() & 0xff;
        } else {
            return -1;
        }
    }
}
