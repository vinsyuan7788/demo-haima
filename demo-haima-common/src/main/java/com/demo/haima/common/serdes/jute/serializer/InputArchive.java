package com.demo.haima.common.serdes.jute.serializer;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.iterator.Index;

import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface InputArchive {
    byte readByte(String tag) throws IOException;
    boolean readBool(String tag) throws IOException;
    int readInt(String tag) throws IOException;
    long readLong(String tag) throws IOException;
    float readFloat(String tag) throws IOException;
    double readDouble(String tag) throws IOException;
    String readString(String tag) throws IOException;
    byte[] readBuffer(String tag) throws IOException;
    void readRecord(Record r, String tag) throws IOException;
    void startRecord(String tag) throws IOException;
    void endRecord(String tag) throws IOException;
    Index startVector(String tag) throws IOException;
    void endVector(String tag) throws IOException;
    Index startMap(String tag) throws IOException;
    void endMap(String tag) throws IOException;
}
