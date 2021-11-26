package com.demo.haima.client.connection.socket;

import com.demo.haima.client.connection.ClientConnection;
import com.demo.haima.client.connection.ClientConnection.SendThread;
import com.demo.haima.client.connection.packet.Packet;
import com.demo.haima.common.io.ByteBufferInputStream;
import com.demo.haima.common.serdes.jute.response.ConnectResponse;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.utils.RecordUtils;
import com.demo.haima.common.utility.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * A ClientConnectionSocket does the lower level communication with a socket
 * implementation.
 *
 * This code has been moved out of ClientConnection so that a Netty implementation can
 * be provided as an alternative to the NIO socket code.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public abstract class ClientConnectionSocket {

    private static final Logger LOG = LoggerFactory.getLogger(ClientConnectionSocket.class);

    private static final int PACKET_LENGTH = Integer.getInteger("jute.maxbuffer", 4096 * 1024);

    /**
     * The port for the client socket to bind.
     * Default value is null, which means the port is left to be auto-decided
     */
    protected Integer clientPort;

    protected SendThread sendThread;
    /**
     * This buffer is only used to read the length of the incoming message.
     */
    protected final ByteBuffer lenBuffer = ByteBuffer.allocateDirect(4);
    /**
     * After the length is read, a new incomingBuffer is allocated in
     * {@link #reallocateIncomingBuffer()} to receive the full message.
     */
    protected ByteBuffer incomingBuffer = lenBuffer;
    /**
     * The sessionId is only available here for Log and Exception messages.
     * Otherwise, the socket doesn't need to know it.
     */
    protected long sessionId;
    /**
     * The flat that signifies if the socket is initialized
     */
    protected boolean initialized;

    private long lastHeard;
    private long lastSend;
    private long now;

    /**
     * This method is used to allocate incoming buffer once it equals {@link #lenBuffer}
     *
     * @throws IOException
     */
    public void reallocateIncomingBuffer() throws IOException {
        int len = incomingBuffer.getInt();
        if (len < 0 || len >= PACKET_LENGTH) {
            throw new IOException("Packet len" + len + " is out of range!");
        }
        incomingBuffer = ByteBuffer.allocate(len);
    }

    /**
     * This method is used to read the result of connect request. <br/>
     * In this method, the buffer that contains the data responded by the server
     * (see {@link NioClientConnectionSocket#doIO(List, LinkedList, ClientConnection)})
     * is de-serialized to a {@link ConnectResponse} instance, which contains necessary
     * information (e.g., session ID, session password) that will be maintained by this client.
     * [Process]
     *
     * @throws IOException
     */
    public void readConnectResult() throws Exception {
        if (LOG.isTraceEnabled()) {
            StringBuilder buf = new StringBuilder("0x[");
            for (byte b : incomingBuffer.array()) {
                buf.append(Integer.toHexString(b) + ",");
            }
            buf.append("]");
            LOG.trace("readConnectResult " + incomingBuffer.remaining() + " " + buf.toString());
        }
        ByteBufferInputStream bbis = new ByteBufferInputStream(incomingBuffer);
        BinaryInputArchive bbia = BinaryInputArchive.getArchive(bbis);
        ConnectResponse connectResponse = new ConnectResponse();
        connectResponse.deserialize(bbia, "connect");
        // information logging here
        LOG.info(LogUtils.getMessage("[Data]", "Response received from server: {}"), RecordUtils.toString(connectResponse));

        // read "is read-only" flag
        boolean isRO = false;
        try {
            isRO = bbia.readBool("readOnly");
        } catch (IOException e) {
            // this is ok -- just a packet from an old server which
            // doesn't contain readOnly field
            LOG.warn("Connected to an old server; r-o mode will be unavailable");
        }

        this.sessionId = connectResponse.getSessionId();
        sendThread.onConnected(connectResponse.getTimeOut(), this.sessionId, connectResponse.getPasswd(), isRO);
    }

    /******************************* Getter and Setter *******************************/

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public void setSendThreadAndSessionId(SendThread sendThread, long sessionId) {
        this.sendThread = sendThread;
        this.sessionId = sessionId;
    }

    /******************************* Heartbeat Processing *******************************/

    public void updateNow() {
        now = System.currentTimeMillis();
    }

    public void updateLastSend() {
        this.lastSend = now;
    }

    public void updateLastHeard() {
        this.lastHeard = now;
    }

    public void updateLastSendAndHeard() {
        this.lastSend = now;
        this.lastHeard = now;
    }

    /**
     * This method is used to get the idle interval between current time and last time heard from the server
     *
     * @return
     */
    public int getIdleRecv() {
        return (int) (now - lastHeard);
    }

    /**
     * This method is used to get the idle interval between current time and last time sent to the server
     *
     * @return
     */
    public int getIdleSend() {
        return (int) (now - lastSend);
    }

    /******************************* Connection Processing *******************************/

    /**
     * This method is used to wake up the connection
     */
    public abstract void wakeupConnection();

    /**
     * This method is used to tell if the client is connected to the server
     *
     * @return
     */
    public abstract boolean isConnected();

    /**
     * This method is used to enable the socket to process read and write event
     */
    public abstract void enableReadWrite();

    /**
     * This method is used to enable the socket to process read event
     */
    public abstract void enableRead();

    /**
     * This method is used to enable the socket to process write event
     */
    public abstract void enableWrite();

    /**
     * This method is used to disable the socket to process write event
     */
    public abstract void disableWrite();

    /**
     * This method is used to transport the packet for further processing
     *
     * @param waitTimeOut
     * @param pendingPacketQueue
     * @param outgoingPacketQueue
     * @param clientConnection
     * @throws Exception
     */
    public abstract void doTransport(int waitTimeOut, List<Packet> pendingPacketQueue, LinkedList<Packet> outgoingPacketQueue, ClientConnection clientConnection) throws Exception;

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to connect the server
     *
     * @param serverAddress
     * @throws Exception
     */
    public abstract void connect(InetSocketAddress serverAddress) throws Exception;

    /**
     * This method is used to clean the registered socket channel and its associated socket
     */
    public abstract void cleanup();

    /**
     * This method is used to close the selector
     */
    public abstract void close();

    /******************************* Getter and Setter *******************************/

    /**
     * This method is used to get the server address
     *
     * @return
     */
    public abstract SocketAddress getRemoteSocketAddress();
}
