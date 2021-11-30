package com.demo.haima.fundamental.utility.data.network.base.packet;

import com.demo.haima.fundamental.utility.data.network.io.stream.ByteBufferInputStream;
import com.demo.haima.fundamental.utility.data.network.base.header.DataTypeCode;
import com.demo.haima.fundamental.utility.exception.DemoException;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/19
 */
public class PacketUtils {

    private PacketUtils() { }

    /**
     * This method is used to wrap data (i.e., header and content) to a packet.
     * The byte buffer in the packet is flipped, hence it can be read directly
     * if needed.
     *
     * @param header
     * @param content
     * @return
     */
    public static Packet<Integer> wrapData(int header, int content)  {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(header);
            dos.writeInt(content);
            ByteBuffer byteBuffer = createByteBuffer(baos.toByteArray());
            return new Packet<>(header, content, byteBuffer);
        } catch (Exception e) {
            throw new DemoException(e);
        }
    }

    /**
     * This method is used to wrap data (i.e., header and content) to a packet.
     * The byte buffer in the packet is flipped, hence it can be read directly
     * if needed.
     *
     * @param header
     * @param content
     * @return
     */
    public static Packet<Long> wrapData(int header, long content)  {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(header);
            dos.writeLong(content);
            ByteBuffer byteBuffer = createByteBuffer(baos.toByteArray());
            return new Packet<>(header, content, byteBuffer);
        } catch (Exception e) {
            throw new DemoException(e);
        }
    }

    /**
     * This method is used to wrap data (i.e., header and content) to a packet.
     * The byte buffer in the packet is flipped, hence it can be read directly
     * if needed.
     *
     * @param header
     * @param content
     * @return
     */
    public static Packet<String> wrapData(int header, String content)  {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(header);
            dos.writeUTF(content);
            ByteBuffer byteBuffer = createByteBuffer(baos.toByteArray());
            return new Packet<>(header, content, byteBuffer);
        } catch (Exception e) {
            throw new DemoException(e);
        }
    }

    /**
     * This method is used to read byte buffer to a packet.
     * The byte buffer in the packet will be flipped, hence there is no need
     * to flip the byte buffer before invoking this method.
     *
     * @param byteBuffer
     * @return
     */
    public static Packet<?> readByteBuffer(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        try (ByteBufferInputStream bbis = new ByteBufferInputStream(byteBuffer);
             DataInputStream dis = new DataInputStream(bbis)) {
            int header = dis.readInt();
            return createPacketByHeader(header, dis, byteBuffer);
        } catch (Exception e) {
            throw new DemoException(e);
        }
    }

    /**
     * This method is used to create a byte buffer
     *
     * @param byteArray
     * @return
     */
    private static ByteBuffer createByteBuffer(byte[] byteArray) {
        if (byteArray == null) {
            return ByteBuffer.allocateDirect(0);
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(byteArray.length);
        byteBuffer.put(byteArray);
        byteBuffer.flip();
        return byteBuffer;
    }

    /**
     * This method is used to create a packet based on header
     *
     * @param header
     * @param dis
     * @param byteBuffer
     * @return
     * @throws Exception
     */
    private static Packet<?> createPacketByHeader(int header, DataInputStream dis, ByteBuffer byteBuffer) throws Exception {
        if (DataTypeCode.INTEGER == header) {
            int content = dis.readInt();
            return new Packet<>(header, content, byteBuffer);
        } else if (DataTypeCode.LONG == header) {
            long content = dis.readLong();
            return new Packet<>(header, content, byteBuffer);
        } else if (DataTypeCode.STRING == header) {
            String content = dis.readUTF();
            return new Packet<>(header, content, byteBuffer);
        } else {
            throw new DemoException("unknown header");
        }
    }
}
