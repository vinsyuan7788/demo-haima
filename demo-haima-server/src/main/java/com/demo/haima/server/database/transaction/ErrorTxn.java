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
public class ErrorTxn implements Record {

    private int err;

    public ErrorTxn() { }
    public ErrorTxn(int err) {
        this.err=err;
    }

    public int getErr() {
        return err;
    }
    public void setErr(int m_) {
        err=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeInt(err,"err");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        err=a_.readInt("err");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(err,"err");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof ErrorTxn)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        ErrorTxn peer = (ErrorTxn) peer_;
        boolean ret = false;
        ret = (err==peer.err);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int)err;
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
        if (!(peer_ instanceof ErrorTxn)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        ErrorTxn peer = (ErrorTxn) peer_;
        int ret = 0;
        ret = (err == peer.err)? 0 :((err<peer.err)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LErrorTxn(i)";
    }
}

