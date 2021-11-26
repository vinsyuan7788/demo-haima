package com.demo.haima.common.serdes.jute.request;

import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.request.operation.Op;
import com.demo.haima.common.serdes.jute.response.header.MultiHeader;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Encodes a composite transaction.  In the wire format, each transaction
 * consists of a single MultiHeader followed by the appropriate request.
 * Each of these MultiHeaders has a type which indicates
 * the type of the following transaction or a negative number if no more transactions
 * are included.
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class MultiTransactionRecord implements Record, Iterable<Op> {

    private List<Op> ops = new ArrayList<Op>();

    public MultiTransactionRecord() { }
    public MultiTransactionRecord(Iterable<Op> ops) {
        for (Op op : ops) {
            add(op);
        }
    }

    public void add(Op op) {
        ops.add(op);
    }
    public int size() {
        return ops.size();
    }

    @Override
    public Iterator<Op> iterator() {
        return ops.iterator() ;
    }
    @Override
    public void serialize(OutputArchive archive, String tag) throws IOException {
        archive.startRecord(this, tag);
        int index = 0 ;
        for (Op op : ops) {
            MultiHeader h = new MultiHeader(op.getType(), false, -1);
            h.serialize(archive, tag);
            switch (op.getType()) {
                case OpCode.create:
                    op.toRequestRecord().serialize(archive, tag);
                    break;
                case OpCode.delete:
                    op.toRequestRecord().serialize(archive, tag);
                    break;
                case OpCode.setData:
                    op.toRequestRecord().serialize(archive, tag);
                    break;
                case OpCode.check:
                    op.toRequestRecord().serialize(archive, tag);
                    break;
                default:
                    throw new IOException("Invalid type of op");
            }
        }
        new MultiHeader(-1, true, -1).serialize(archive, tag);
        archive.endRecord(this, tag);
    }
    @Override
    public void deserialize(InputArchive archive, String tag) throws IOException {
        archive.startRecord(tag);
        MultiHeader h = new MultiHeader();
        h.deserialize(archive, tag);

        while (!h.getDone()) {
            switch (h.getType()) {
                case OpCode.create:
                    CreateRequest cr = new CreateRequest();
                    cr.deserialize(archive, tag);
                    add(Op.create(cr.getPath(), cr.getData(), cr.getFlags()));
                    break;
                case OpCode.delete:
                    DeleteRequest dr = new DeleteRequest();
                    dr.deserialize(archive, tag);
                    add(Op.delete(dr.getPath(), dr.getVersion()));
                    break;
                case OpCode.setData:
                    SetDataRequest sdr = new SetDataRequest();
                    sdr.deserialize(archive, tag);
                    add(Op.setData(sdr.getPath(), sdr.getData(), sdr.getVersion()));
                    break;
                case OpCode.check:
                    CheckVersionRequest cvr = new CheckVersionRequest();
                    cvr.deserialize(archive, tag);
                    add(Op.check(cvr.getPath(), cvr.getVersion()));
                    break;
                default:
                    throw new IOException("Invalid type of op");
            }
            h.deserialize(archive, tag);
        }
        archive.endRecord(tag);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MultiTransactionRecord)) {
            return false;
        }

        MultiTransactionRecord that = (MultiTransactionRecord) o;

        if (ops != null) {
            Iterator<Op> other = that.ops.iterator();
            for (Op op : ops) {
                boolean hasMoreData = other.hasNext();
                if (!hasMoreData) {
                    return false;
                }
                Op otherOp = other.next();
                if (!op.equals(otherOp)) {
                    return false;
                }
            }
            return !other.hasNext();
        } else {
            return that.ops == null;
        }
    }
    @Override
    public int hashCode() {
        int h = 1023;
        for (Op op : ops) {
            h = h * 25 + op.hashCode();
        }
        return h;
    }
}

