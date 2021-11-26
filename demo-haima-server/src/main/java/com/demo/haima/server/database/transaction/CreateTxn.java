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
public class CreateTxn implements Record {

    private String path;
    private byte[] data;
    private int parentCVersion;

    public CreateTxn() { }
    public CreateTxn(String path, byte[] data, int parentCVersion) {
        this.path=path;
        this.data=data;
        this.parentCVersion=parentCVersion;
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
    public int getParentCVersion() {
        return parentCVersion;
    }
    public void setParentCVersion(int m_) {
        parentCVersion=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeString(path,"path");
        a_.writeBuffer(data,"data");
        a_.writeInt(parentCVersion,"parentCVersion");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        path=a_.readString("path");
        data=a_.readBuffer("data");
        parentCVersion=a_.readInt("parentCVersion");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s =
                    new ByteArrayOutputStream();
            CsvOutputArchive a_ =
                    new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeString(path,"path");
            a_.writeBuffer(data,"data");
            a_.writeInt(parentCVersion,"parentCVersion");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof CreateTxn)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        CreateTxn peer = (CreateTxn) peer_;
        boolean ret = false;
        ret = path.equals(peer.path);
        if (!ret) {
            return ret;
        }
        ret = Utils.bufEquals(data,peer.data);
        if (!ret) {
            return ret;
        }
        ret = (parentCVersion==peer.parentCVersion);
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
        ret = (int)parentCVersion;
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
        throw new UnsupportedOperationException("comparing CreateTxn is unimplemented");
    }

    public static String signature() {
        return "LCreateTxn(sB[LACL(iLId(ss))]zi)";
    }
}

