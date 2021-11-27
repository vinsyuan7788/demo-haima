package com.demo.haima.fundamental.client.duplex.nonblocking;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.client.duplex.nonblocking.socket.DemoDuplexClientSocket;
import com.demo.haima.fundamental.client.duplex.nonblocking.socket.NioDemoDuplexClientSocket;
import com.demo.haima.fundamental.utils.data.network.definition.ByteBufferType;
import com.demo.haima.fundamental.utils.data.network.definition.OperationType;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import com.demo.haima.fundamental.utils.data.network.request.body.RequestBody;
import com.demo.haima.fundamental.utils.data.network.request.body.SnowflakeIdRequestBody;
import com.demo.haima.fundamental.utils.data.network.request.body.TransmitDataRequestBody;
import com.demo.haima.fundamental.utils.data.network.request.header.RequestHeader;
import com.demo.haima.fundamental.utils.data.network.response.body.ResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.SnowflakeIdResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.TransmitDataResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * This client is used to serve as client that can send data to server. <br/>
 * It implements a duplex communication with server with one thread and one client socket
 * channel each thread. This idea is widely adopted in the industry and implemented by some
 * open-source frameworks (e.g., zoo-keeper, etc.). Of course this demo can be further
 * extended in terms of design (e.g., introducing or integrating heart-beat, session management,
 * data processing (including structure, transaction, sharding, etc.), exception processing,
 * configuration, statistics, monitoring, etc.)
 *
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioDemoDuplexClient extends DemoDuplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(NioDemoDuplexClient.class);

    public DemoDuplexClientSocket clientSocket;

    public NioDemoDuplexClient(SocketAddress serverAddressToConnect) {
        clientSocket = new NioDemoDuplexClientSocket(this, serverAddressToConnect);
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
    public <T> Boolean write(T data) {
        if (data == null) {
            return null;
        }
        long startTimeMillis = System.currentTimeMillis();
        RequestHeader requestHeader = RequestHeader.create(OperationType.TRANSMIT_DATA);
        RequestBody requestBody = TransmitDataRequestBody.create(data);
        Packet packet = Packet.create(requestHeader, requestBody, ByteBufferType.DIRECT);
        System.out.println("Time elapsed for creating a packet: " + (System.currentTimeMillis() - startTimeMillis) + "ms");
        ResponseBody responseBody = clientSocket.submit(packet);
        if (responseBody == null) {
            return null;
        } else {
            return ((TransmitDataResponseBody) responseBody).isSuccess();
        }
    }

    @Override
    public Long getSnowflakeId(int appCode) {
        long startTimeMillis = System.currentTimeMillis();
        RequestHeader requestHeader = RequestHeader.create(OperationType.SNOWFLAKE_ID);
        RequestBody requestBody = SnowflakeIdRequestBody.create(appCode);
        Packet packet = Packet.create(requestHeader, requestBody, ByteBufferType.DIRECT);
        System.out.println("Time elapsed for creating a packet: " + (System.currentTimeMillis() - startTimeMillis) + "ms");
        ResponseBody responseBody = clientSocket.submit(packet);
        if (responseBody == null) {
            return null;
        } else {
            return ((SnowflakeIdResponseBody) responseBody).getSnowflakeId();
        }
    }
}
