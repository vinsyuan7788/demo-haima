package com.demo.haima.server.database.transaction;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.CsvOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;
import com.demo.haima.common.serdes.jute.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class Txn implements Record {

    private int type;
    private byte[] data;

    public Txn() { }
    public Txn(int type, byte[] data) {
        this.type=type;
        this.data=data;
    }

    public int getType() {
        return type;
    }
    public void setType(int m_) {
        type=m_;
    }
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] m_) {
        data=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeInt(type,"type");
        a_.writeBuffer(data,"data");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        type=a_.readInt("type");
        data=a_.readBuffer("data");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(type,"type");
            a_.writeBuffer(data,"data");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof Txn)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        Txn peer = (Txn) peer_;
        boolean ret = false;
        ret = (type==peer.type);
        if (!ret) {
            return ret;
        }
        ret = Utils.bufEquals(data,peer.data);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int)type;
        result = 37 * result + ret;
        ret = Arrays.toString(data).hashCode();
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
        if (!(peer_ instanceof Txn)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        Txn peer = (Txn) peer_;
        int ret = 0;
        ret = (type == peer.type)? 0 :((type<peer.type)?-1:1);
        if (ret != 0) {
            return ret;
        }
        {
            byte[] my = data;
            byte[] ur = peer.data;
            ret = Utils.compareBytes(my,0,my.length,ur,0,ur.length);
        }
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LTxn(iB)";
    }
}

