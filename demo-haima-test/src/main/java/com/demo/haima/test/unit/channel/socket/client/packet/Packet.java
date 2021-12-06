package com.demo.haima.test.unit.channel.socket.client.packet;

import java.util.UUID;

/**
 * @author Vince Yuan
 * @date 2021/12/6
 */
public class Packet {

    private String id;
    private String dataToSend;
    private String dataReceived;

    public Packet(String dataToSend) {
        this.id = UUID.randomUUID().toString();
        this.dataToSend = dataToSend;
    }

    public void setDataReceived(String dataReceived) {
        this.dataReceived = dataReceived;
    }

    public String getId() {
        return id;
    }
    public String getDataToSend() {
        return dataToSend;
    }
    public String getDataReceived() {
        return dataReceived;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "id='" + id + '\'' +
                ", dataToSend='" + dataToSend + '\'' +
                ", dataReceived='" + dataReceived + '\'' +
                '}';
    }
}
