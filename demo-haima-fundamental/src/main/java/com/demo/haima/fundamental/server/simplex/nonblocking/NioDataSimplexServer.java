package com.demo.haima.fundamental.server.simplex.nonblocking;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.server.simplex.nonblocking.socket.NioDataSimplexServerSocket;
import com.demo.haima.fundamental.server.simplex.nonblocking.socket.DataSimplexServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to serve as server that can receive data from client. <br/>
 * It is an advanced version of {@link NioBaseSimplexServer}. However, the server here is
 * not aware of the data type of data sent from client, since client does not send
 * any information regarding the data type. Therefore, the data received by server
 * cannot be properly handled actually. The server here is served as a bad (but still
 * referable) example regarding data manipulation.
 * For a better example, see {@link NioPacketSimplexServer}.
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class NioDataSimplexServer extends DataSimplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(NioDataSimplexServer.class);

    private DataSimplexServerSocket serverSocket;

    public NioDataSimplexServer(int portToBind) {
        this.serverSocket = new NioDataSimplexServerSocket(portToBind);
    }

    @Override
    public void startServer() {
        serverSocket.startSocket();
        LOG.info(LogUtils.getMessage("Server is started"));
    }

    @Override
    public void closeServer() {
        serverSocket.closeSocket();
        LOG.info(LogUtils.getMessage("Server is closed"));
    }
}
