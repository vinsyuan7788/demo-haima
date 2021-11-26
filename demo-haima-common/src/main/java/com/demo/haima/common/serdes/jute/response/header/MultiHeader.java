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
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class MultiHeader implements Record {

    private int type;
    private boolean done;
    private int err;
    
    public MultiHeader() { }
    public MultiHeader(int type, boolean done, int err) {
        this.type=type;
        this.done=done;
        this.err=err;
    }
    
    public int getType() {
        return type;
    }
    public void setType(int m_) {
        type=m_;
    }
    public boolean getDone() {
        return done;
    }
    public void setDone(boolean m_) {
        done=m_;
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
        a_.writeInt(type,"type");
        a_.writeBool(done,"done");
        a_.writeInt(err,"err");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        type=a_.readInt("type");
        done=a_.readBool("done");
        err=a_.readInt("err");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(type,"type");
            a_.writeBool(done,"done");
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
        if (!(peer_ instanceof MultiHeader)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        MultiHeader peer = (MultiHeader) peer_;
        boolean ret = false;
        ret = (type==peer.type);
        if (!ret) {
            return ret;
        }
        ret = (done==peer.done);
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
        ret = (int)type;
        result = 37 * result + ret;
        ret = (done)?0:1;
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
        if (!(peer_ instanceof MultiHeader)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        MultiHeader peer = (MultiHeader) peer_;
        int ret = 0;
        ret = (type == peer.type)? 0 :((type<peer.type)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (done == peer.done)? 0 : (done?1:-1);
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
        return "LMultiHeader(izi)";
    }
}

