package com.demo.haima.server.database.transaction.header;

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
public class TxnHeader implements Record {
    
    private long clientId;
    private long time;
    private int type;
    
    public TxnHeader() { }
    public TxnHeader(long clientId, long time, int type) {
        this.clientId=clientId;
        this.time=time;
        this.type=type;
    }
    
    public long getClientId() {
        return clientId;
    }
    public void setClientId(long m_) {
        clientId=m_;
    }
    public long getTime() {
        return time;
    }
    public void setTime(long m_) {
        time=m_;
    }
    public int getType() {
        return type;
    }
    public void setType(int m_) {
        type=m_;
    }
    
    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeLong(clientId,"clientId");
        a_.writeLong(time,"time");
        a_.writeInt(type,"type");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        clientId=a_.readLong("clientId");
        time=a_.readLong("time");
        type=a_.readInt("type");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeLong(clientId,"clientId");
            a_.writeLong(time,"time");
            a_.writeInt(type,"type");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof TxnHeader)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        TxnHeader peer = (TxnHeader) peer_;
        boolean ret = false;
        ret = (clientId==peer.clientId);
        if (!ret) {
            return ret;
        }
        ret = (time==peer.time);
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
        ret = (int) (clientId^(clientId>>>32));
        result = 37 * result + ret;
        ret = (int) (time^(time>>>32));
        result = 37 * result + ret;
        ret = (int)type;
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
        if (!(peer_ instanceof TxnHeader)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        TxnHeader peer = (TxnHeader) peer_;
        int ret = 0;
        ret = (clientId == peer.clientId)? 0 :((clientId<peer.clientId)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (time == peer.time)? 0 :((time<peer.time)?-1:1);
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
        return "LTxnHeader(lilli)";
    }
}

