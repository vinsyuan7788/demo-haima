package com.demo.haima.common.serdes.jute.serializer;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.iterator.BinaryIndex;
import com.demo.haima.common.serdes.jute.iterator.Index;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class BinaryInputArchive implements InputArchive {

    public static final int MAX_BUFFER = Integer.getInteger("jute.maxbuffer", 0xfffff);

    private static final String UNREASONABLE_LENGTH= "Unreasonable length = ";

    private DataInput in;

    public static BinaryInputArchive getArchive(InputStream strm) {
        return new BinaryInputArchive(new DataInputStream(strm));
    }

    /** Creates a new instance of BinaryInputArchive */
    public BinaryInputArchive(DataInput in) {
        this.in = in;
    }

    @Override
    public byte readByte(String tag) throws IOException {
        return in.readByte();
    }

    @Override
    public boolean readBool(String tag) throws IOException {
        return in.readBoolean();
    }

    @Override
    public int readInt(String tag) throws IOException {
        return in.readInt();
    }

    @Override
    public long readLong(String tag) throws IOException {
        return in.readLong();
    }

    @Override
    public float readFloat(String tag) throws IOException {
        return in.readFloat();
    }

    @Override
    public double readDouble(String tag) throws IOException {
        return in.readDouble();
    }

    @Override
    public String readString(String tag) throws IOException {
        int len = in.readInt();
        if (len == -1) {
            return null;
        }
        checkLength(len);
        byte b[] = new byte[len];
        in.readFully(b);
        return new String(b, "UTF8");
    }

    @Override
    public byte[] readBuffer(String tag) throws IOException {
        int len = readInt(tag);
        if (len == -1) {
            return null;
        }
        checkLength(len);
        byte[] arr = new byte[len];
        in.readFully(arr);
        return arr;
    }

    @Override
    public void readRecord(Record r, String tag) throws IOException {
        r.deserialize(this, tag);
    }

    @Override
    public void startRecord(String tag) throws IOException {}

    @Override
    public void endRecord(String tag) throws IOException {}

    @Override
    public Index startVector(String tag) throws IOException {
        int len = readInt(tag);
        if (len == -1) {
            return null;
        }
        return new BinaryIndex(len);
    }

    @Override
    public void endVector(String tag) throws IOException {}

    @Override
    public Index startMap(String tag) throws IOException {
        return new BinaryIndex(readInt(tag));
    }

    @Override
    public void endMap(String tag) throws IOException {}

    // Since this is a rough sanity check, add some padding to maxBuffer to
    // make up for extra fields, etc. (otherwise e.g. clients may be able to
    // write buffers larger than we can read from disk!)
    private void checkLength(int len) throws IOException {
        if (len < 0 || len > MAX_BUFFER + 1024) {
            throw new IOException(UNREASONABLE_LENGTH + len);
        }
    }
}
