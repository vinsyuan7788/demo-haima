package com.demo.haima.fundamental.utils.data.network.io.wrapper;

import com.demo.haima.fundamental.utils.data.network.io.stream.ByteBufferInputStream;
import com.demo.haima.fundamental.utils.exception.DemoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public class BinaryInputWrapper implements InputWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(BinaryInputWrapper.class);

    private ByteBufferInputStream inputStream;
    private DataInputStream dataInput;

    private BinaryInputWrapper(ByteBufferInputStream inputStream) {
        this.inputStream = inputStream;
        this.dataInput = new DataInputStream(this.inputStream);
    }

    public static BinaryInputWrapper create(ByteBuffer byteBuffer) {
        return new BinaryInputWrapper(new ByteBufferInputStream(byteBuffer));
    }

    @Override
    public boolean readBool() {
        try {
            return dataInput.readBoolean();
        } catch (Throwable t) {
            throw new DemoException("Read error", t);
        }
    }

    @Override
    public int readInt() {
        try {
            return dataInput.readInt();
        } catch (Throwable t) {
            throw new DemoException("Read error", t);
        }
    }

    @Override
    public long readLong() {
        try {
            return dataInput.readLong();
        } catch (Throwable t) {
            throw new DemoException("Read error", t);
        }
    }

    @Override
    public String readString() {
        try {
            return dataInput.readUTF();
        } catch (Throwable t) {
            throw new DemoException("Read error", t);
        }
    }
}
