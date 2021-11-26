package com.demo.haima.common.serdes.jute.request;

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
public class GetChildren2Request implements Record {

    private String path;
    private boolean watch;
    
    public GetChildren2Request() { }
    public GetChildren2Request(String path, boolean watch) {
        this.path=path;
        this.watch=watch;
    }
    
    public String getPath() {
        return path;
    }
    public void setPath(String m_) {
        path=m_;
    }
    public boolean getWatch() {
        return watch;
    }
    public void setWatch(boolean m_) {
        watch=m_;
    }
    
    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeString(path,"path");
        a_.writeBool(watch,"watch");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        path=a_.readString("path");
        watch=a_.readBool("watch");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeString(path,"path");
            a_.writeBool(watch,"watch");
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
        if (!(peer_ instanceof GetChildren2Request)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        GetChildren2Request peer = (GetChildren2Request) peer_;
        boolean ret = false;
        ret = path.equals(peer.path);
        if (!ret) {
            return ret;
        }
        ret = (watch==peer.watch);
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
        ret = (watch)?0:1;
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
        if (!(peer_ instanceof GetChildren2Request)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        GetChildren2Request peer = (GetChildren2Request) peer_;
        int ret = 0;
        ret = path.compareTo(peer.path);
        if (ret != 0) {
            return ret;
        }
        ret = (watch == peer.watch)? 0 : (watch?1:-1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LGetChildren2Request(sz)";
    }
}

