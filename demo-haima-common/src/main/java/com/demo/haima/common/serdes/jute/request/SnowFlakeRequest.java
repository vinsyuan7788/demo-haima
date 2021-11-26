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
 * @date 2021/11/12
 */
public class SnowFlakeRequest implements Record {

    private int app;

    public SnowFlakeRequest() { }
    public SnowFlakeRequest(int app) {
        this.app = app;
    }

    public int getApp() {
        return app;
    }
    public void setApp(int app) {
        this.app = app;
    }

    @Override
    public void serialize(OutputArchive archive, String tag) throws IOException {
        archive.startRecord(this, tag);
        archive.writeInt(app, "app");
        archive.endRecord(this, tag);
    }
    @Override
    public void deserialize(InputArchive archive, String tag) throws IOException {
        archive.startRecord(tag);
        app = archive.readInt("app");
        archive.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(app,"app");
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
        if (!(peer_ instanceof SnowFlakeRequest)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        SnowFlakeRequest peer = (SnowFlakeRequest) peer_;
        boolean ret = false;
        ret = (app==peer.app);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int) app;
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
        if (!(peer_ instanceof SnowFlakeRequest)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        SnowFlakeRequest peer = (SnowFlakeRequest) peer_;
        int ret = 0;
        ret = (app == peer.app)? 0 :((app<peer.app)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }
    
    public static String signature() {
        return "LSnowFlakeRequest(ii)";
    }
}
