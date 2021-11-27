package com.demo.haima.fundamental.utils.data.network.base;

import com.demo.haima.fundamental.client.simplex.nonblocking.NioPacketSimplexClient;
import com.demo.haima.fundamental.server.simplex.nonblocking.NioPacketSimplexServer;
import com.demo.haima.fundamental.utils.data.network.base.packet.PacketUtils;

import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class DataUtils {

    private DataUtils() { }

    /**
     * This method is used to wrap data to byte array. <br/>
     *
     * @param data the data that needs to be sent to server
     * @param <T>
     * @return the wrapped byte buffer carrying the data
     */
    public static <T> ByteBuffer wrapData(T data) {
        if (data == null) {
            return ByteBuffer.wrap(new byte[0]);
        }
        if (data instanceof Integer) {
            return ByteBuffer.wrap(((Integer) data).toString().getBytes());
        } else if (data instanceof Long) {
            return ByteBuffer.wrap(((Long) data).toString().getBytes());
        } else if (data instanceof String) {
            return ByteBuffer.wrap(((String) data).getBytes());
        } else {
            // Not implemented for now for the rest of data type
            return ByteBuffer.wrap(new byte[0]);
        }
    }

    /**
     * This method is used to read the data contained in the byte buffer. <br/>
     * This is the down-side of this implementation. If client hopes to send
     * any type of data to server, server will NOT be able to know how to convert
     * the data back to its original type (e.g., int, long, String, etc.). To
     * address this problem, it is best to send a "header" to tell server what
     * type of data will be received.
     * See {@link NioPacketSimplexClient} and {@link NioPacketSimplexServer} (or {@link PacketUtils})
     * for more details. <br/>
     * For now, here converts the data to String type universally.
     *
     * @param byteBuffer
     * @param <T>
     * @return
     */
    public static <T> T readByteBuffer(ByteBuffer byteBuffer) {
        byte[] byteArray = new byte[byteBuffer.limit()];
        byteBuffer.get(byteArray);
        return (T) new String(byteArray);
    }
}
