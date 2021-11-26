package com.demo.haima.server.database.data.statistics;

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
 * @date 2021/11/9
 */
public class StatPersisted implements Record {
    
    private long czxid;
    private long mzxid;
    private long ctime;
    private long mtime;
    private int version;
    private int cversion;
    private int aversion;
    private long ephemeralOwner;
    private long pzxid;
    
    public StatPersisted() { }
    public StatPersisted(long czxid, long mzxid, long ctime, long mtime, int version, int cversion, int aversion, long ephemeralOwner, long pzxid) {
        this.czxid=czxid;
        this.mzxid=mzxid;
        this.ctime=ctime;
        this.mtime=mtime;
        this.version=version;
        this.cversion=cversion;
        this.aversion=aversion;
        this.ephemeralOwner=ephemeralOwner;
        this.pzxid=pzxid;
    }
    
    public long getCzxid() {
        return czxid;
    }
    public void setCzxid(long m_) {
        czxid=m_;
    }
    public long getMzxid() {
        return mzxid;
    }
    public void setMzxid(long m_) {
        mzxid=m_;
    }
    public long getCtime() {
        return ctime;
    }
    public void setCtime(long m_) {
        ctime=m_;
    }
    public long getMtime() {
        return mtime;
    }
    public void setMtime(long m_) {
        mtime=m_;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int m_) {
        version=m_;
    }
    public int getCversion() {
        return cversion;
    }
    public void setCversion(int m_) {
        cversion=m_;
    }
    public int getAversion() {
        return aversion;
    }
    public void setAversion(int m_) {
        aversion=m_;
    }
    public long getEphemeralOwner() {
        return ephemeralOwner;
    }
    public void setEphemeralOwner(long m_) {
        ephemeralOwner=m_;
    }
    public long getPzxid() {
        return pzxid;
    }
    public void setPzxid(long m_) {
        pzxid=m_;
    }
    
    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeLong(czxid,"czxid");
        a_.writeLong(mzxid,"mzxid");
        a_.writeLong(ctime,"ctime");
        a_.writeLong(mtime,"mtime");
        a_.writeInt(version,"version");
        a_.writeInt(cversion,"cversion");
        a_.writeInt(aversion,"aversion");
        a_.writeLong(ephemeralOwner,"ephemeralOwner");
        a_.writeLong(pzxid,"pzxid");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        czxid=a_.readLong("czxid");
        mzxid=a_.readLong("mzxid");
        ctime=a_.readLong("ctime");
        mtime=a_.readLong("mtime");
        version=a_.readInt("version");
        cversion=a_.readInt("cversion");
        aversion=a_.readInt("aversion");
        ephemeralOwner=a_.readLong("ephemeralOwner");
        pzxid=a_.readLong("pzxid");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeLong(czxid,"czxid");
            a_.writeLong(mzxid,"mzxid");
            a_.writeLong(ctime,"ctime");
            a_.writeLong(mtime,"mtime");
            a_.writeInt(version,"version");
            a_.writeInt(cversion,"cversion");
            a_.writeInt(aversion,"aversion");
            a_.writeLong(ephemeralOwner,"ephemeralOwner");
            a_.writeLong(pzxid,"pzxid");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof StatPersisted)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        StatPersisted peer = (StatPersisted) peer_;
        boolean ret = false;
        ret = (czxid==peer.czxid);
        if (!ret) {
            return ret;
        }
        ret = (mzxid==peer.mzxid);
        if (!ret) {
            return ret;
        }
        ret = (ctime==peer.ctime);
        if (!ret) {
            return ret;
        }
        ret = (mtime==peer.mtime);
        if (!ret) {
            return ret;
        }
        ret = (version==peer.version);
        if (!ret) {
            return ret;
        }
        ret = (cversion==peer.cversion);
        if (!ret) {
            return ret;
        }
        ret = (aversion==peer.aversion);
        if (!ret) {
            return ret;
        }
        ret = (ephemeralOwner==peer.ephemeralOwner);
        if (!ret) {
            return ret;
        }
        ret = (pzxid==peer.pzxid);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int) (czxid^(czxid>>>32));
        result = 37 * result + ret;
        ret = (int) (mzxid^(mzxid>>>32));
        result = 37 * result + ret;
        ret = (int) (ctime^(ctime>>>32));
        result = 37 * result + ret;
        ret = (int) (mtime^(mtime>>>32));
        result = 37 * result + ret;
        ret = (int)version;
        result = 37 * result + ret;
        ret = (int)cversion;
        result = 37 * result + ret;
        ret = (int)aversion;
        result = 37 * result + ret;
        ret = (int) (ephemeralOwner^(ephemeralOwner>>>32));
        result = 37 * result + ret;
        ret = (int) (pzxid^(pzxid>>>32));
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
        if (!(peer_ instanceof StatPersisted)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        StatPersisted peer = (StatPersisted) peer_;
        int ret = 0;
        ret = (czxid == peer.czxid)? 0 :((czxid<peer.czxid)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (mzxid == peer.mzxid)? 0 :((mzxid<peer.mzxid)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (ctime == peer.ctime)? 0 :((ctime<peer.ctime)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (mtime == peer.mtime)? 0 :((mtime<peer.mtime)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (version == peer.version)? 0 :((version<peer.version)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (cversion == peer.cversion)? 0 :((cversion<peer.cversion)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (aversion == peer.aversion)? 0 :((aversion<peer.aversion)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (ephemeralOwner == peer.ephemeralOwner)? 0 :((ephemeralOwner<peer.ephemeralOwner)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (pzxid == peer.pzxid)? 0 :((pzxid<peer.pzxid)?-1:1);
        if (ret != 0) {
            return ret;
        }
        return ret;
    }

    public static String signature() {
        return "LStatPersisted(lllliiill)";
    }

    /**
     * This method is used to copy StatPersisted instance
     *
     * @param from original instance
     * @param to new instance
     */
    public static void copy(StatPersisted from, StatPersisted to) {
        to.setAversion(from.getAversion());
        to.setCtime(from.getCtime());
        to.setCversion(from.getCversion());
        to.setCzxid(from.getCzxid());
        to.setMtime(from.getMtime());
        to.setMzxid(from.getMzxid());
        to.setPzxid(from.getPzxid());
        to.setVersion(from.getVersion());
        to.setEphemeralOwner(from.getEphemeralOwner());
    }
}

