package com.demo.haima.fundamental.client.simplex.nonblocking.socket;

import com.demo.haima.fundamental.utils.data.network.packet.Packet;

import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public interface SerdesSimplexClientSocket extends BaseSimplexClientSocket {

    /****************************** Data Processing ******************************/

    /**
     * This method is used to submit the packet to send to server
     *
     * @param packet
     */
    void submit(Packet packet);

    /****************************** Setup and Shutdown ******************************/

    /**
     * This method is used to create a socket channel
     *
     * @return
     */
    SocketChannel createSocketChannel();

    /**
     * This method is used to configure the socket channel. <br/>
     * For {@link NioSerdesSimplexClientSocket}, we just set TCP_NODELAY
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
     * in {@link NioSerdesSimplexClientSocket}, so far the reason why it occurs is still unknown since
     * we invoke the provided method to configure the socket. It occurs occasionally and when
     * it occurs, it does not affect server to receive data sent from client so far, hence for
     * now it remains to be observed. See {@link #configureSocketChannel(SocketChannel)} for
     * more details.
     *
     * @param t
     */
    void handleConfigureSocketChannelThrowable(Throwable t);

    /**
     * This method is used to handle the throwable during connecting to server.
     * For {@link ClosedByInterruptException}, it is raised due to the interruption
     * operation of {@link #closeSocket()}. See {@link #closeSocket()} for more details.
     * It may cause a miss-out of a socket to process. To avoid this interruption, it is
     * best to avoid using blocking operation of {@link NioSerdesSimplexClientSocket#outgoingPacketQueue}.
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
