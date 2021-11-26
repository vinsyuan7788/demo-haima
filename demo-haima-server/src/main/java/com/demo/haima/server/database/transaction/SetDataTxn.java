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
public class SetDataTxn implements Record {

    private String path;
    private byte[] data;
    private int version;

    public SetDataTxn() { }
    public SetDataTxn(String path, byte[] data, int version) {
        this.path=path;
        this.data=data;
        this.version=version;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String m_) {
        path=m_;
    }
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] m_) {
        data=m_;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int m_) {
        version=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeString(path,"path");
        a_.writeBuffer(data,"data");
        a_.writeInt(version,"version");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        path=a_.readString("path");
        data=a_.readBuffer("data");
        version=a_.readInt("version");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeString(path,"path");
            a_.writeBuffer(data,"data");
            a_.writeInt(version,"version");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof SetDataTxn)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        SetDataTxn peer = (SetDataTxn) peer_;
        boolean ret = false;
        ret = path.equals(peer.path);
        if (!ret) {
            return ret;
        }
        ret = Utils.bufEquals(data,peer.data);
        if (!ret) {
            return ret;
        }
        ret = (version==peer.version);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = path.hashCode();
        result = 37 * result + ret;
        ret = Arrays.toString(data).hashCode();
        result = 37 * result + ret;
        ret = (int)version;
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
        if (!(peer_ instanceof SetDataTxn)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        SetDataTxn peer = (SetDataTxn) peer_;
        int ret = 0;
        ret = path.compareTo(peer.path);
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
        ret = (version == peer.version)? 0 :((version<peer.version)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LSetDataTxn(sBi)";
    }
}

