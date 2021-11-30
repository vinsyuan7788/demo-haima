package com.demo.haima.fundamental.utility.data.network.io.wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public class BinaryOutputWrapper implements OutputWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(BinaryOutputWrapper.class);

    private ByteArrayOutputStream outputStream;
    private DataOutputStream dataOutput;

    private BinaryOutputWrapper(ByteArrayOutputStream outputStream) {
        this.outputStream = outputStream;
        this.dataOutput = new DataOutputStream(this.outputStream);
    }

    public static BinaryOutputWrapper create() {
        return new BinaryOutputWrapper(new ByteArrayOutputStream());
    }

    @Override
    public void writeBool(boolean value) {
        try {
            dataOutput.writeBoolean(value);
        } catch (Throwable t) {
            LOG.error("Write error", t);
        }
    }

    @Override
    public void writeInt(int value) {
        try {
            dataOutput.writeInt(value);
        } catch (Throwable t) {
            LOG.error("Write error", t);
        }
    }

    @Override
    public void writeLong(long value) {
        try {
            dataOutput.writeLong(value);
        } catch (Throwable t) {
            LOG.error("Write error", t);
        }
    }

    @Override
    public void writeString(String value) {
        try {
            dataOutput.writeUTF(value);
        } catch (Throwable t) {
            LOG.error("Write error", t);
        }
    }

    public byte[] getByteArray() {
        return outputStream.toByteArray();
    }
}
