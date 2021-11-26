package com.demo.haima.common.serdes.jute.response;

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
 * @date 2021/11/11
 */
public class GetChildrenResponse implements Record {

    private List<String> children;

    public GetChildrenResponse() { }
    public GetChildrenResponse(List<String> children) {
        this.children=children;
    }

    public List<String> getChildren() {
        return children;
    }
    public void setChildren(List<String> m_) {
        children=m_;
    }

    @Override
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this,tag);
        {
            a_.startVector(children,"children");
            if (children!= null) {          int len1 = children.size();
                for(int vidx1 = 0; vidx1<len1; vidx1++) {
                    String e1 = (String) children.get(vidx1);
                    a_.writeString(e1,"e1");
                }
            }
            a_.endVector(children,"children");
        }
        a_.endRecord(this,tag);
    }
    @Override
    public void deserialize(InputArchive a_, String tag) throws IOException {
        a_.startRecord(tag);
        {
            Index vidx1 = a_.startVector("children");
            if (vidx1!= null) {          children=new ArrayList<String>();
                for (; !vidx1.done(); vidx1.incr()) {
                    String e1;
                    e1=a_.readString("e1");
                    children.add(e1);
                }
            }
            a_.endVector("children");
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
                a_.startVector(children,"children");
                if (children!= null) {          int len1 = children.size();
                    for(int vidx1 = 0; vidx1<len1; vidx1++) {
                        String e1 = (String) children.get(vidx1);
                        a_.writeString(e1,"e1");
                    }
                }
                a_.endVector(children,"children");
            }
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
        if (!(peer_ instanceof GetChildrenResponse)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        GetChildrenResponse peer = (GetChildrenResponse) peer_;
        boolean ret = false;
        ret = children.equals(peer.children);
        if (!ret) {
            return ret;
        }
        return ret;
    }
    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = children.hashCode();
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
        throw new UnsupportedOperationException("comparing GetChildrenResponse is unimplemented");
    }

    public static String signature() {
        return "LGetChildrenResponse([s])";
    }
}

