package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.socket;

import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.NioSerdesSimplexClient;
import com.demo.haima.test.integration.demo.utils.state.client.nio.ClientState;

import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.BlockingQueue;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public interface BaseSimplexClientSocket {

    /************************ Setup and Shutdown ************************/

    /**
     * This method is used to start the socket
     */
    void startSocket();

    /**
     * This method is used to close the socket <br/>
     * For {@link NioBaseSimplexClientSocket}, {@link NioDataSimplexClientSocket},
     * {@link NioPacketSimplexClientSocket}, the communication with server needs to be ensured
     * before closing the resources (e.g., socket channel, socket, etc.) to avoid
     * {@link ClosedChannelException}. See {@link #handleRunningThrowable(Throwable)}
     * for more details <br/>
     * For {@link NioSerdesSimplexClientSocket}, we need to interrupt {@link BlockingQueue#take()}
     * (if we are using it) manually to ensure the socket thread can exit the running method
     * correctly. It will cause an {@link InterruptedException}, which is OK in this scenario.
     * See {@link #handleRunningThrowable(Throwable)} for more details <br/>
     */
    void closeSocket();

    /**
     * This method is used to handle throwable during initializing. <br/>
     * This method is used to handle the throwable during configuring socket channel.<br/>
     * For {@link SocketException} with message "Invalid argument: no further information",
     * in {@link NioBaseSimplexClientSocket}, {@link NioDataSimplexClientSocket}, {@link NioPacketSimplexClientSocket},
     * so far the reason why it occurs is still unknown since we invoke the provided method to
     * configure the socket. It occurs occasionally and when it occurs, it does not affect server
     * to receive data sent from client so far, hence for now it remains to be observed.
     * See their respective constructors for more details.
     *
     * @param t
     */
    void handleInitializingThrowable(Throwable t);

    /**
     * This method is used to handle throwable during starting
     *
     * @param t
     */
    void handleStartingThrowable(Throwable t);

    /**
     * This method is used handle throwable during closing
     *
     * @param t
     */
    void handleClosingThrowable(Throwable t);

    /************************ Data Processing ************************/

    /**
     * This method is used handle throwable during closing. <br/>
     * For {@link ClosedChannelException}, in {@link NioBaseSimplexClientSocket},
     * {@link NioDataSimplexClientSocket}, {@link NioPacketSimplexClientSocket}, we need
     * to ensure that the communication with server completes before closing
     * resources (e.g., socket channel, socket, etc.), otherwise, this exception
     * will occur. To avoid this exception, see {@link #closeSocket()}. <br/>
     * For {@link InterruptedException}, in {@link NioSerdesSimplexClientSocket}, when
     * the size of {@link NioSerdesSimplexClientSocket#outgoingPacketQueue} and
     * {@link NioSerdesSimplexClientSocket#connectionIdAndProcessingPacketMap} becomes zero,
     * {@link NioSerdesSimplexClient#clientState} may not be switched to
     * {@link ClientState#CLOSED} in time (not before this socket comes to
     * {@link BlockingQueue#take()}, if we are using such blocking structure),
     * which will cause the socket thread to wait indefinitely. To address this
     * problem (see {@link #closeSocket()}), it will cause this exception, which is
     * fine and can be ignored in this scenario. <br/>
     *
     * @param t
     */
    void handleRunningThrowable(Throwable t);
}
