package com.demo.haima.fundamental.utility.data.network.base.packet;

import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/19
 */
public class Packet<T> {

    private int header;
    private T content;
    private ByteBuffer byteBuffer;

    public Packet(int header, T content, ByteBuffer byteBuffer) {
        this.header = header;
        this.content = content;
        this.byteBuffer = byteBuffer;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "header=" + header +
                ", content=" + content +
                ", byteBuffer=" + byteBuffer +
                '}';
    }

    public int getHeader() {
        return header;
    }

    public T getContent() {
        return content;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }
}
