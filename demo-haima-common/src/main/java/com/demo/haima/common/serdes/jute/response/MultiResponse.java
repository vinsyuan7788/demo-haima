package com.demo.haima.common.serdes.jute.response;

import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.definition.OpResult;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.response.header.MultiHeader;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles the response from a multi request.  Such a response consists of
 * a sequence of responses each prefixed by a MultiResponse that indicates
 * the type of the response.  The end of the list is indicated by a MultiHeader
 * with a negative type.  Each individual response is in the same format as
 * with the corresponding operation in the original request list.
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class MultiResponse implements Record, Iterable<OpResult> {

    private List<OpResult> results = new ArrayList<OpResult>();

    public void add(OpResult x) {
        results.add(x);
    }
    public int size() {
        return results.size();
    }
    public List<OpResult> getResultList() {
        return results;
    }

    @Override
    public Iterator<OpResult> iterator() {
        return results.iterator();
    }
    @Override
    public void serialize(OutputArchive archive, String tag) throws IOException {
        archive.startRecord(this, tag);

        int index = 0;
        for (OpResult result : results) {
            int err = result.getType() == OpCode.error ? ((OpResult.ErrorResult)result).getErr() : 0;

            new MultiHeader(result.getType(), false, err).serialize(archive, tag);

            switch (result.getType()) {
                case OpCode.create:
                    new CreateResponse(((OpResult.CreateResult) result).getPath()).serialize(archive, tag);
                    break;
                case OpCode.delete:
                case OpCode.check:
                    break;
                case OpCode.setData:
                    new SetDataResponse(((OpResult.SetDataResult) result).getStat()).serialize(archive, tag);
                    break;
                case OpCode.error:
                    new ErrorResponse(((OpResult.ErrorResult) result).getErr()).serialize(archive, tag);
                    break;
                default:
                    throw new IOException("Invalid type " + result.getType() + " in MultiResponse");
            }
        }
        new MultiHeader(-1, true, -1).serialize(archive, tag);
        archive.endRecord(this, tag);
    }
    @Override
    public void deserialize(InputArchive archive, String tag) throws IOException {
        results = new ArrayList<OpResult>();

        archive.startRecord(tag);
        MultiHeader h = new MultiHeader();
        h.deserialize(archive, tag);
        while (!h.getDone()) {
            switch (h.getType()) {
                case OpCode.create:
                    CreateResponse cr = new CreateResponse();
                    cr.deserialize(archive, tag);
                    results.add(new OpResult.CreateResult(cr.getPath()));
                    break;

                case OpCode.delete:
                    results.add(new OpResult.DeleteResult());
                    break;

                case OpCode.setData:
                    SetDataResponse sdr = new SetDataResponse();
                    sdr.deserialize(archive, tag);
                    results.add(new OpResult.SetDataResult(sdr.getStat()));
                    break;

                case OpCode.check:
                    results.add(new OpResult.CheckResult());
                    break;

                case OpCode.error:
                    //FIXME: need way to more cleanly serialize/deserialize exceptions
                    ErrorResponse er = new ErrorResponse();
                    er.deserialize(archive, tag);
                    results.add(new OpResult.ErrorResult(er.getErr()));
                    break;

                default:
                    throw new IOException("Invalid type " + h.getType() + " in MultiResponse");
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
        if (!(o instanceof MultiResponse)) {
            return false;
        }

        MultiResponse other = (MultiResponse) o;

        if (results != null) {
            Iterator<OpResult> i = other.results.iterator();
            for (OpResult result : results) {
                if (i.hasNext()) {
                    if (!result.equals(i.next())) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return !i.hasNext();
        }
        else {
            return other.results == null;
        }
    }
    @Override
    public int hashCode() {
        int hash = results.size();
        for (OpResult result : results) {
            hash = (hash * 35) + result.hashCode();
        }
        return hash;
    }
}

