package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.NioPacketSimplexTest;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.socket.NioPacketSimplexClientSocket;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.socket.PacketSimplexClientSocket;
import com.demo.haima.test.integration.demo.utils.data.network.base.header.DataTypeCode;
import com.demo.haima.test.integration.demo.utils.data.network.base.packet.Packet;
import com.demo.haima.test.integration.demo.utils.data.network.base.packet.PacketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * This client is used to serve as client that can send data to server. <br/>
 * The data manipulation here is a good idea, since it introduces "header" that will
 * provide necessary information regarding the actual content to server, hence when
 * server received the entire data, serve will know how to process the actual content
 * properly.
 * However, the down-side of this implementation is that one-socket corresponds to one
 * thread, which means in highly-intensive IO, we need multiple threads to complete them
 * (i.e., instantiate a client and start it each time, see {@link NioPacketSimplexTest}),
 * which is very expensive for system resource (e.g., context switch between kernel and
 * user mode, etc.). To address this problem, see {@link NioSerdesSimplexClient}
 *
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioPacketSimplexClient extends PacketSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(NioPacketSimplexClient.class);

    public PacketSimplexClientSocket clientSocket;

    public NioPacketSimplexClient(SocketAddress serverAddressToConnect) {
        clientSocket = new NioPacketSimplexClientSocket(serverAddressToConnect);
    }

    @Override
    public void startClient() {
        clientSocket.startSocket();
        LOG.info(LogUtils.getMessage("Client is started"));
    }

    @Override
    public void closeClient() {
        clientSocket.closeSocket();
        LOG.info(LogUtils.getMessage("Client is closed"));
    }

    @Override
    public <T> void write(T data) {
        long startTimeMillis = System.currentTimeMillis();
        if (data == null) {
            return;
        }
        Packet packet;
        if (data instanceof Integer) {
            packet = PacketUtils.wrapData(DataTypeCode.INTEGER, (Integer) data);
        } else if (data instanceof Long) {
            packet = PacketUtils.wrapData(DataTypeCode.LONG, (Long) data);
        } else if (data instanceof String) {
            packet = PacketUtils.wrapData(DataTypeCode.STRING, (String) data);
        } else {
            // Other data types are not-yet implemented, but they are necessary if needed
            packet = null;
        }
        System.out.println("Time elapsed for creating a packet: " + (System.currentTimeMillis() - startTimeMillis) + "ms");
        clientSocket.submit(packet);
    }
}
