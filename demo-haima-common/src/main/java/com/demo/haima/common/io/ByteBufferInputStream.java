package com.demo.haima.common.io;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.utils.RecordUtils;
import com.demo.haima.common.utility.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class ByteBufferInputStream extends InputStream {

    private static final Logger LOG = LoggerFactory.getLogger(ByteBufferInputStream.class);

    ByteBuffer bb;

    public ByteBufferInputStream(ByteBuffer bb) {
        this.bb = bb;
    }

    @Override
    public int read() throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        return bb.get() & 0xff;
    }

    @Override
    public int available() throws IOException {
        return bb.remaining();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        if (len > bb.remaining()) {
            len = bb.remaining();
        }
        bb.get(b, off, len);
        return len;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        long newPos = bb.position() + n;
        if (newPos > bb.remaining()) {
            n = bb.remaining();
        }
        bb.position(bb.position() + (int) n);
        return n;
    }

    /**
     * This method is used to deserialize the data stored in the buffer to the Record instance
     *
     * @param byteBuffer
     * @param record
     * @throws IOException
     */
    public static void byteBuffer2Record(ByteBuffer byteBuffer, Record record) throws IOException {
        record.deserialize(BinaryInputArchive.getArchive(new ByteBufferInputStream(byteBuffer)), "request");
        LOG.info(LogUtils.getMessage("[Data]", "Request received from client: {}"), RecordUtils.toString(record));
    }
}
