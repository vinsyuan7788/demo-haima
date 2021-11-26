package com.demo.haima.common.serdes.jute.response.header;

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
 * This class represents the response header sent from server to client
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class ResponseHeader implements Record {

    private int beid;
    private int err;

    public ResponseHeader() { }
    public ResponseHeader(int beid, int err) {
        this.beid=beid;
        this.err=err;
    }
    public int getBeid() {
        return beid;
    }
    public void setBeid(int m_) {
        beid=m_;
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
        a_.writeInt(beid,"beid");
        a_.writeInt(err,"err");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        beid=a_.readInt("beid");
        err=a_.readInt("err");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(beid,"beid");
            a_.writeInt(err,"err");
            a_.endRecord(this,"");
            String infoString = new String(s.toByteArray(), "UTF-8");
            return getClass().getSimpleName() + "|" + infoString;
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof ResponseHeader)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        ResponseHeader peer = (ResponseHeader) peer_;
        boolean ret = false;
        ret = (beid==peer.beid);
        if (!ret) {
            return ret;
        }
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
        ret = (int)beid;
        result = 37 * result + ret;
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
        if (!(peer_ instanceof ResponseHeader)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        ResponseHeader peer = (ResponseHeader) peer_;
        int ret = 0;
        ret = (beid == peer.beid)? 0 :((beid<peer.beid)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (err == peer.err)? 0 :((err<peer.err)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LResponseHeader(ili)";
    }
}
