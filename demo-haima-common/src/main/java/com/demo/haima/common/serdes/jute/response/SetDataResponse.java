package com.demo.haima.common.serdes.jute.response;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.CsvOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;
import com.demo.haima.common.serdes.jute.statistics.Stat;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class SetDataResponse implements Record {

    private Stat stat;

    public SetDataResponse() { }
    public SetDataResponse(Stat stat) {
        this.stat=stat;
    }

    public Stat getStat() {
        return stat;
    }
    public void setStat(Stat m_) {
        stat=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeRecord(stat,"stat");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        stat= new Stat();
        a_.readRecord(stat,"stat");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeRecord(stat,"stat");
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
        if (!(peer_ instanceof SetDataResponse)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        SetDataResponse peer = (SetDataResponse) peer_;
        boolean ret = false;
        ret = stat.equals(peer.stat);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = stat.hashCode();
        result = 37*result + ret;
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
        if (!(peer_ instanceof SetDataResponse)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        SetDataResponse peer = (SetDataResponse) peer_;
        int ret = 0;
        ret = stat.compareTo(peer.stat);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LSetDataResponse(LStat(lllliiiliil))";
    }
}

