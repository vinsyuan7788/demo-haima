package com.demo.haima.fundamental.client.duplex.nonblocking.socket;

import com.demo.haima.fundamental.server.duplex.nonblocking.socket.DemoDuplexServerSocket;
import com.demo.haima.fundamental.utility.data.network.packet.Packet;
import com.demo.haima.fundamental.utility.data.network.response.body.ResponseBody;

import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public interface DemoDuplexClientSocket extends BaseDuplexClientSocket {

    /****************************** Data Processing ******************************/

    /**
     * This method is used to submit the packet to send to server
     *
     * @param packet
     * @return
     */
    ResponseBody submit(Packet packet);

    /**
     * This method is used to select keys whose channels are ready for registered events.
     * See {@link DemoDuplexServerSocket#selectKeys(Selector)} for more details.
     *
     * @param selector
     * @return
     * @throws Exception
     */
    void selectKeys(Selector selector) throws Exception;

    /**
     * This method is used to get selected keys whose channels are ready for
     * registered events.
     * See {@link DemoDuplexServerSocket#getSelectedKeys(Selector)} for more details.
     *
     * @param selector
     * @return
     * @throws Exception
     */
    Set<SelectionKey> getSelectedKeys(Selector selector);

    /**
     * This method is used to process selected keys whose channels are ready for
     * registered events.
     * See {@link DemoDuplexServerSocket#processSelectedKeys(Set)} for more details.
     *
     * @param selectedKeys
     * @throws Exception
     */
    void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception;

    /**
     * This method is used to process accept event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processAcceptEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process connect event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processConnectableEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process read event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processReadEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process write event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processWritableEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process the received packet
     *
     * @param receivedPacket
     */
    void processReceivedPacket(Packet receivedPacket);

    /****************************** Setup and Shutdown ******************************/

    /**
     * This method is used to create a socket channel
     *
     * @return
     */
    SocketChannel createSocketChannel();

    /**
     * This method is used to configure the socket channel. <br/>
     * For {@link NioDemoDuplexClientSocket}, we just set TCP_NODELAY
     * and SO_LINGER, but it may occasionally cause a {@link SocketException}.
     * See {@link #handleConfigureSocketChannelThrowable} for more details.
     *
     * @param socketChannel
     */
    void configureSocketChannel(SocketChannel socketChannel);

    /**
     * This method is used to connect the socket channel to server
     *
     * @param socketChannel
     */
    void connectToServer(SocketChannel socketChannel);

    /**
     * This method is used to close the socket channel
     *
     * @param socketChannel
     */
    void closeSocketChannel(SocketChannel socketChannel);

    /**
     * This method is used to handle the throwable during creating a socket channel
     *
     * @param t
     */
    void handleCreateSocketChannelThrowable(Throwable t);

    /**
     * This method is used to handle the throwable during configuring socket channel.<br/>
     * For {@link SocketException} with message "Invalid argument: no further information",
     * in {@link NioDemoDuplexClientSocket}, so far the reason why it occurs is still unknown since
     * we invoke the provided method to configure the socket. It occurs occasionally and when
     * it occurs, it does not affect server to receive data sent from client so far, hence for
     * now it remains to be observed. See {@link #configureSocketChannel(SocketChannel)} for
     * more details.
     *
     * @param t
     */
    void handleConfigureSocketChannelThrowable(Throwable t);

    /**
     * This method is used to handle the throwable during connecting to server
     *
     * @param t
     */
    void handleConnectToServerThrowable(Throwable t);

    /**
     * This method is used to handle the throwable during closing socket channel
     *
     * @param t
     */
    void handleCloseSocketChannelThrowable(Throwable t);
}
