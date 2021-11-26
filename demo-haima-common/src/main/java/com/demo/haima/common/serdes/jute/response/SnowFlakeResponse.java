package com.demo.haima.common.serdes.jute.response;

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
public class SnowFlakeResponse implements Record {

    /**
     * Snowflake ID in this response
     */
    private long sfId;

    public SnowFlakeResponse() { }
    public SnowFlakeResponse(long sfId) {
        this.sfId = sfId;
    }

    public long getSfId() {
        return sfId;
    }
    public void setSfId(long sfId) {
        this.sfId = sfId;
    }

    @Override
    public void serialize(OutputArchive archive, String tag) throws IOException {
        archive.startRecord(this, tag);
        archive.writeLong(sfId, "sfId");
        archive.endRecord(this, tag);
    }
    @Override
    public void deserialize(InputArchive archive, String tag) throws IOException {
        archive.startRecord(tag);
        sfId = archive.readLong("sfId");
        archive.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeLong(sfId, "sfId");
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
        if (!(peer_ instanceof SnowFlakeResponse)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        SnowFlakeResponse peer = (SnowFlakeResponse) peer_;
        boolean ret = false;
        ret = (sfId==peer.sfId);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int) sfId;
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
        if (!(peer_ instanceof SnowFlakeResponse)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        SnowFlakeResponse peer = (SnowFlakeResponse) peer_;
        int ret = 0;
        ret = (sfId == peer.sfId)? 0 :((sfId<peer.sfId)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LSnowFlakeResponse(ii)";
    }
}
