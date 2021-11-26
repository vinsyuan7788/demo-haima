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
public class CheckVersionTxn implements Record {

    private String path;
    private int version;

    public CheckVersionTxn() { }
    public CheckVersionTxn(String path, int version) {
        this.path=path;
        this.version=version;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String m_) {
        path=m_;
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
        a_.writeInt(version,"version");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        path=a_.readString("path");
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
        if (!(peer_ instanceof CheckVersionTxn)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        CheckVersionTxn peer = (CheckVersionTxn) peer_;
        boolean ret = false;
        ret = path.equals(peer.path);
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
    public int compareTo (Object peer_) throws ClassCastException {
        if (!(peer_ instanceof CheckVersionTxn)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        CheckVersionTxn peer = (CheckVersionTxn) peer_;
        int ret = 0;
        ret = path.compareTo(peer.path);
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
        return "LCheckVersionTxn(si)";
    }
}

