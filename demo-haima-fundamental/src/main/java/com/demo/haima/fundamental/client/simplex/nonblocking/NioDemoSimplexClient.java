package com.demo.haima.fundamental.client.simplex.nonblocking;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.client.duplex.nonblocking.NioDemoDuplexClient;
import com.demo.haima.fundamental.client.simplex.nonblocking.socket.DemoSimplexClientSocket;
import com.demo.haima.fundamental.client.simplex.nonblocking.socket.NioDemoSimplexClientSocket;
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
 * Here addresses the problem of {@link NioSerdesSimplexClient}, which is that this server
 * uses only one client socket channel in one thread for simplex communication, which
 * is resource-efficient in terms of both thread and socket.
 * Notice that in simplex communication, client is only responsible to send data, and it does
 * not care (or one should say is unable to care) the process result of the data sent to server
 * (e.g., if server receives it or even processes it successfully). For a more intact interaction
 * with server, it is better to use a duplex communication. <br/>
 * See {@link NioDemoDuplexClient})
 * for more details.
 *
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioDemoSimplexClient extends DemoSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(NioDemoSimplexClient.class);

    public DemoSimplexClientSocket clientSocket;

    public NioDemoSimplexClient(SocketAddress serverAddressToConnect) {
        clientSocket = new NioDemoSimplexClientSocket(this, serverAddressToConnect);
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
