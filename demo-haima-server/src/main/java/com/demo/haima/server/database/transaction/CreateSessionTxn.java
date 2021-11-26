package com.demo.haima.server.database.transaction;


import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.CsvOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class CreateSessionTxn implements Record {

    private int timeOut;

    public CreateSessionTxn() { }
    public CreateSessionTxn(int timeOut) {
        this.timeOut=timeOut;
    }

    public int getTimeOut() {
        return timeOut;
    }
    public void setTimeOut(int m_) {
        timeOut=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeInt(timeOut,"timeOut");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        timeOut=a_.readInt("timeOut");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(timeOut,"timeOut");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof CreateSessionTxn)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        CreateSessionTxn peer = (CreateSessionTxn) peer_;
        boolean ret = false;
        ret = (timeOut==peer.timeOut);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int)timeOut;
        result = 37 * result + ret;
        return result;
    }

    public void write(DataOutput out) throws IOException {
        BinaryOutputArchive archive = new BinaryOutputArchive(out);
        serialize(archive, "");
    }
    public void readFields(DataInput in) throws IOException {
        BinaryInputArchive archive = new BinaryInputArchive(in);
        deserialize(archive, "");
    }
    public int compareTo(Object peer_) throws ClassCastException {
        if (!(peer_ instanceof CreateSessionTxn)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        CreateSessionTxn peer = (CreateSessionTxn) peer_;
        int ret = 0;
        ret = (timeOut == peer.timeOut)? 0 :((timeOut<peer.timeOut)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LCreateSessionTxn(i)";
    }
}

