package com.demo.haima.fundamental.client.simplex.nonblocking;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.client.simplex.nonblocking.socket.NioSerdesSimplexClientSocket;
import com.demo.haima.fundamental.client.simplex.nonblocking.socket.SerdesSimplexClientSocket;
import com.demo.haima.fundamental.utils.data.network.definition.OperationType;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import com.demo.haima.fundamental.utils.data.network.request.body.RequestBody;
import com.demo.haima.fundamental.utils.data.network.request.body.TransmitDataRequestBody;
import com.demo.haima.fundamental.utils.data.network.request.header.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * This client is used to serve as client that can send data to server. <br/>
 * This client use one thread to manipulate socket as needed if there is any
 * data that needs to be sent to the server, which saves a lot of OS resources
 * and around 5 to 10 times faster than {@link NioPacketSimplexClient} in highly-intensive
 * situation.
 * Besides, this client uses the packet that has been further modified from the one
 * used by {@link NioPacketSimplexClient}, which is much simpler when using the packet (while
 * the design and implementation of the packet is more complex and elegant). Even though
 * in one-time situation, it will take much more time to create the packet, but in
 * highly-intensive situation, the overall time cost is much lower than {@link NioPacketSimplexClient}.
 * However, this client is still not efficient in terms of sockets, since it will use a
 * socket each time there is a data to send, to make it resource-efficient on both thread and
 * socket, see {@link NioDemoSimplexClient}.
 *
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioSerdesSimplexClient extends SerdesSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(NioSerdesSimplexClient.class);

    public SerdesSimplexClientSocket clientSocket;

    public NioSerdesSimplexClient(SocketAddress serverAddressToConnect) {
        clientSocket = new NioSerdesSimplexClientSocket(this, serverAddressToConnect);
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
        if (data == null) {
            return;
        }
        long startTimeMillis = System.currentTimeMillis();
        RequestHeader requestHeader = RequestHeader.create(OperationType.TRANSMIT_DATA);
        RequestBody requestBody = TransmitDataRequestBody.create(data);
        Packet packet = Packet.create(requestHeader, requestBody);
        System.out.println("Time elapsed for creating a packet: " + (System.currentTimeMillis() - startTimeMillis) + "ms");
        clientSocket.submit(packet);
    }
}
