package com.demo.haima.common.serdes.jute.response;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.CsvOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;
import com.demo.haima.common.serdes.jute.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * This class represents the connect response sent from server to client
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class ConnectResponse implements Record {

    private int protocolVersion;
    private int timeOut;
    private long sessionId;
    private byte[] passwd;
    
    public ConnectResponse() { }
    public ConnectResponse(int protocolVersion, int timeOut, long sessionId, byte[] passwd) {
        this.protocolVersion=protocolVersion;
        this.timeOut=timeOut;
        this.sessionId=sessionId;
        this.passwd=passwd;
    }
    
    public int getProtocolVersion() {
        return protocolVersion;
    }
    public void setProtocolVersion(int m_) {
        protocolVersion=m_;
    }
    public int getTimeOut() {
        return timeOut;
    }
    public void setTimeOut(int m_) {
        timeOut=m_;
    }
    public long getSessionId() {
        return sessionId;
    }
    public void setSessionId(long m_) {
        sessionId=m_;
    }
    public byte[] getPasswd() {
        return passwd;
    }
    public void setPasswd(byte[] m_) {
        passwd=m_;
    }
    
    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        a_.writeInt(protocolVersion,"protocolVersion");
        a_.writeInt(timeOut,"timeOut");
        a_.writeLong(sessionId,"sessionId");
        a_.writeBuffer(passwd,"passwd");
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        protocolVersion=a_.readInt("protocolVersion");
        timeOut=a_.readInt("timeOut");
        sessionId=a_.readLong("sessionId");
        passwd=a_.readBuffer("passwd");
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(protocolVersion,"protocolVersion");
            a_.writeInt(timeOut,"timeOut");
            a_.writeLong(sessionId,"sessionId");
            a_.writeBuffer(passwd,"passwd");
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
        if (!(peer_ instanceof ConnectResponse)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        ConnectResponse peer = (ConnectResponse) peer_;
        boolean ret = false;
        ret = (protocolVersion==peer.protocolVersion);
        if (!ret) {
            return ret;
        }
        ret = (timeOut==peer.timeOut);
        if (!ret) {
            return ret;
        }
        ret = (sessionId==peer.sessionId);
        if (!ret) {
            return ret;
        }
        ret = Utils.bufEquals(passwd,peer.passwd);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int)protocolVersion;
        result = 37 * result + ret;
        ret = (int)timeOut;
        result = 37 * result + ret;
        ret = (int) (sessionId^(sessionId>>>32));
        result = 37 * result + ret;
        ret = Arrays.toString(passwd).hashCode();
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
        if (!(peer_ instanceof ConnectResponse)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        ConnectResponse peer = (ConnectResponse) peer_;
        int ret = 0;
        ret = (protocolVersion == peer.protocolVersion)? 0 :((protocolVersion<peer.protocolVersion)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (timeOut == peer.timeOut)? 0 :((timeOut<peer.timeOut)?-1:1);
        if (ret != 0) {
            return ret;
        }
        ret = (sessionId == peer.sessionId)? 0 :((sessionId<peer.sessionId)?-1:1);
        if (ret != 0) {
            return ret;
        }
        {
            byte[] my = passwd;
            byte[] ur = peer.passwd;
            ret = Utils.compareBytes(my,0,my.length,ur,0,ur.length);
        }
        if (ret != 0) {
            return ret;
        }
        return ret;
    }
    
    public static String signature() {
        return "LConnectResponse(iilB)";
    }
}
