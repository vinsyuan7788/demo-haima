package com.demo.haima.common.serdes.jute.response;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.CsvOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;
import com.demo.haima.common.serdes.jute.statistics.Stat;
import com.demo.haima.common.serdes.jute.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class GetDataResponse implements Record {
    
    private byte[] data;
    private Stat stat;
    
    public GetDataResponse() { }
    public GetDataResponse(byte[] data, Stat stat) {
        this.data=data;
        this.stat=stat;
    }
    
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] m_) {
        data=m_;
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
        a_.writeBuffer(data,"data");
        a_.writeRecord(stat,"stat");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        data=a_.readBuffer("data");
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
            a_.writeBuffer(data,"data");
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
        if (!(peer_ instanceof GetDataResponse)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        GetDataResponse peer = (GetDataResponse) peer_;
        boolean ret = false;
        ret = Utils.bufEquals(data,peer.data);
        if (!ret) {
            return ret;
        }
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
        ret = Arrays.toString(data).hashCode();
        result = 37 * result + ret;
        ret = stat.hashCode();
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
        if (!(peer_ instanceof GetDataResponse)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        GetDataResponse peer = (GetDataResponse) peer_;
        int ret = 0;
        {
            byte[] my = data;
            byte[] ur = peer.data;
            ret = Utils.compareBytes(my,0,my.length,ur,0,ur.length);
        }
        if (ret != 0) {
            return ret;
        }
        ret = stat.compareTo(peer.stat);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LGetDataResponse(BLStat(lllliiiliil))";
    }
}

