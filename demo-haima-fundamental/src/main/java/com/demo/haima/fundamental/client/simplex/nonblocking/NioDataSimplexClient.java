package com.demo.haima.fundamental.client.simplex.nonblocking;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.client.simplex.nonblocking.socket.DataSimplexClientSocket;
import com.demo.haima.fundamental.client.simplex.nonblocking.socket.NioDataSimplexClientSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * This client is used to serve as client that can send data to server. <br/>
 * It is an advanced version of {@link NioBaseSimplexClient}. However, the data sent to
 * server cannot be properly processed by server, since server is not aware of
 * the data type of data sent from client. Hence, this client here is served as
 * a bad (but still referable) example regarding data manipulation.
 * For a better example, see {@link NioPacketSimplexClient}.
 *
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioDataSimplexClient extends DataSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(NioDataSimplexClient.class);

    public DataSimplexClientSocket clientSocket;

    public NioDataSimplexClient(SocketAddress serverAddressToConnect) {
        clientSocket = new NioDataSimplexClientSocket(serverAddressToConnect);
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
        clientSocket.submit(data);
    }
}
