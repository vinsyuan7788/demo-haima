package com.demo.haima.server.database.transaction;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.CsvOutputArchive;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.iterator.Index;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class MultiTxn implements Record {

    private List<Txn> txns;

    public MultiTxn() { }
    public MultiTxn(List<Txn> txns) {
        this.txns=txns;
    }

    public List<Txn> getTxns() {
        return txns;
    }
    public void setTxns(List<Txn> m_) {
        txns=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        {
            a_.startVector(txns,"txns");
            if (txns!= null) {          int len1 = txns.size();
                for(int vidx1 = 0; vidx1<len1; vidx1++) {
                    Txn e1 = (Txn) txns.get(vidx1);
                    a_.writeRecord(e1,"e1");
                }
            }
            a_.endVector(txns,"txns");
        }
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        {
            Index vidx1 = a_.startVector("txns");
            if (vidx1!= null) {          txns=new ArrayList<Txn>();
                for (; !vidx1.done(); vidx1.incr()) {
                    Txn e1;
                    e1= new Txn();
                    a_.readRecord(e1,"e1");
                    txns.add(e1);
                }
            }
            a_.endVector("txns");
        }
        a_.endRecord(tag);
    }
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            CsvOutputArchive a_ = new CsvOutputArchive(s);
            a_.startRecord(this,"");
            {
                a_.startVector(txns,"txns");
                if (txns!= null) {          int len1 = txns.size();
                    for(int vidx1 = 0; vidx1<len1; vidx1++) {
                        Txn e1 = (Txn) txns.get(vidx1);
                        a_.writeRecord(e1,"e1");
                    }
                }
                a_.endVector(txns,"txns");
            }
            a_.endRecord(this,"");
            return new String(s.toByteArray(), "UTF-8");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    @Override
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof MultiTxn)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        MultiTxn peer = (MultiTxn) peer_;
        boolean ret = false;
        ret = txns.equals(peer.txns);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = txns.hashCode();
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
        throw new UnsupportedOperationException("comparing MultiTxn is unimplemented");
    }

    public static String signature() {
        return "LMultiTxn([LTxn(iB)])";
    }
}

