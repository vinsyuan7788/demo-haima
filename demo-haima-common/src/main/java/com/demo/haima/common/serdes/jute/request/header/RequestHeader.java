package com.demo.haima.common.serdes.jute.request.header;

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
 * This class represents the request header sent from client to server
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class RequestHeader implements Record {

    private int xid;
    private int type;

    public RequestHeader() { }
    public RequestHeader(int xid, int type) {
        this.xid=xid;
        this.type=type;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeInt(xid,"xid");
        a_.writeInt(type,"type");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        xid=a_.readInt("xid");
        type=a_.readInt("type");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a = new CsvOutputArchive(s);
            a.startRecord(this,"");
            a.writeInt(xid,"xid");
            a.writeInt(type,"type");
            a.endRecord(this,"");
            String infoString = new String(s.toByteArray(), "UTF-8");
            return getClass().getSimpleName() + "|" + infoString;
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof RequestHeader)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        RequestHeader peer = (RequestHeader) peer_;
        boolean ret = false;
        ret = (xid==peer.xid);
        if (!ret) {
            return ret;
        }
        ret = (type==peer.type);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int)xid;
        result = 37 * result + ret;
        ret = (int)type;
        result = 37 * result + ret;
        return result;
    }

    public int getXid() {
        return xid;
    }
    public void setXid(int m_) {
        xid=m_;
    }
    public int getType() {
        return type;
    }
    public void setType(int m_) {
        type=m_;
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
        if (!(peer_ instanceof RequestHeader)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        RequestHeader peer = (RequestHeader) peer_;
        int ret = 0;
        ret = (xid == peer.xid)? 0 :((xid<peer.xid)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (type == peer.type)? 0 :((type<peer.type)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LRequestHeader(ii)";
    }
}
