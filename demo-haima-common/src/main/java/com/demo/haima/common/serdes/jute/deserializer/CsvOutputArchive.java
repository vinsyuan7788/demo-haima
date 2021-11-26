package com.demo.haima.common.serdes.jute.deserializer;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.TreeMap;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class CsvOutputArchive implements OutputArchive {

    private PrintStream stream;
    private boolean isFirst = true;

    public static CsvOutputArchive getArchive(OutputStream strm) throws UnsupportedEncodingException {
        return new CsvOutputArchive(strm);
    }

    /** Creates a new instance of CsvOutputArchive */
    public CsvOutputArchive(OutputStream out) throws UnsupportedEncodingException {
        stream = new PrintStream(out, true, "UTF-8");
    }

    @Override
    public void writeByte(byte b, String tag) throws IOException {
        writeLong((long)b, tag);
    }

    @Override
    public void writeBool(boolean b, String tag) throws IOException {
        printCommaUnlessFirst();
        String val = b ? "T" : "F";
        stream.print(val);
        throwExceptionOnError(tag);
    }

    @Override
    public void writeInt(int i, String tag) throws IOException {
        writeLong((long)i, tag);
    }

    @Override
    public void writeLong(long l, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print(l);
        throwExceptionOnError(tag);
    }

    @Override
    public void writeFloat(float f, String tag) throws IOException {
        writeDouble((double)f, tag);
    }

    @Override
    public void writeDouble(double d, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print(d);
        throwExceptionOnError(tag);
    }

    @Override
    public void writeString(String s, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print(Utils.toCSVString(s));
        throwExceptionOnError(tag);
    }

    @Override
    public void writeBuffer(byte buf[], String tag)
            throws IOException {
        printCommaUnlessFirst();
        stream.print(Utils.toCSVBuffer(buf));
        throwExceptionOnError(tag);
    }

    @Override
    public void writeRecord(Record r, String tag) throws IOException {
        if (r == null) {
            return;
        }
        r.serialize(this, tag);
    }

    @Override
    public void startRecord(Record r, String tag) throws IOException {
        if (tag != null && !"".equals(tag)) {
            printCommaUnlessFirst();
            stream.print("s{");
            isFirst = true;
        }
    }

    @Override
    public void endRecord(Record r, String tag) throws IOException {
        if (tag == null || "".equals(tag)) {
            stream.print("\n");
            isFirst = true;
        } else {
            stream.print("}");
            isFirst = false;
        }
    }

    @Override
    public void startVector(List v, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print("v{");
        isFirst = true;
    }

    @Override
    public void endVector(List v, String tag) throws IOException {
        stream.print("}");
        isFirst = false;
    }

    @Override
    public void startMap(TreeMap v, String tag) throws IOException {
        printCommaUnlessFirst();
        stream.print("m{");
        isFirst = true;
    }

    @Override
    public void endMap(TreeMap v, String tag) throws IOException {
        stream.print("}");
        isFirst = false;
    }

    private void throwExceptionOnError(String tag) throws IOException {
        if (stream.checkError()) {
            throw new IOException("Error serializing "+tag);
        }
    }

    private void printCommaUnlessFirst() {
        if (!isFirst) {
            stream.print(",");
        }
        isFirst = false;
    }
}
