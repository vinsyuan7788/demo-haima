package com.demo.haima.test.integration.demo.utils.data.network.io.stream;

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
            return byteBuffer.get();
        } else {
            return -1;
        }
    }
}
