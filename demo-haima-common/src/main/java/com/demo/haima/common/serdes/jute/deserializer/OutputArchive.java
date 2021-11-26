package com.demo.haima.common.serdes.jute.deserializer;

import com.demo.haima.common.serdes.jute.Record;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface OutputArchive {
    void writeByte(byte b, String tag) throws IOException;
    void writeBool(boolean b, String tag) throws IOException;
    void writeInt(int i, String tag) throws IOException;
    void writeLong(long l, String tag) throws IOException;
    void writeFloat(float f, String tag) throws IOException;
    void writeDouble(double d, String tag) throws IOException;
    void writeString(String s, String tag) throws IOException;
    void writeBuffer(byte buf[], String tag) throws IOException;
    void writeRecord(Record r, String tag) throws IOException;
    void startRecord(Record r, String tag) throws IOException;
    void endRecord(Record r, String tag) throws IOException;
    void startVector(List v, String tag) throws IOException;
    void endVector(List v, String tag) throws IOException;
    void startMap(TreeMap v, String tag) throws IOException;
    void endMap(TreeMap v, String tag) throws IOException;
}
