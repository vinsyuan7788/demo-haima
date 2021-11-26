package com.demo.haima.client.connection.socket;

import com.demo.haima.client.connection.ClientConnection;
import com.demo.haima.client.connection.packet.Packet;
import com.demo.haima.client.exception.EndOfStreamException;
import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.utility.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class NioClientConnectionSocket extends ClientConnectionSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioClientConnectionSocket.class);

    private final Selector selector = Selector.open();
    private SelectionKey registeredKey;

    public NioClientConnectionSocket() throws IOException {
        super();
    }

    @Override
    public void wakeupConnection() {
        selector.wakeup();
    }

    @Override
    public boolean isConnected() {
        return registeredKey != null;
    }

    @Override
    public synchronized void enableReadWrite() {
        registeredKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void enableRead() {
        int i = registeredKey.interestOps();
        if ((i & SelectionKey.OP_READ) == 0) {
            registeredKey.interestOps(i | SelectionKey.OP_READ);
        }
    }

    @Override
    public synchronized void enableWrite() {
        int i = registeredKey.interestOps();
        if ((i & SelectionKey.OP_WRITE) == 0) {
            registeredKey.interestOps(i | SelectionKey.OP_WRITE);
        }
    }

    @Override
    public synchronized void disableWrite() {
        int i = registeredKey.interestOps();
        if ((i & SelectionKey.OP_WRITE) != 0) {
            registeredKey.interestOps(i & (~SelectionKey.OP_WRITE));
        }
    }

    /**
     * In this method, for the reason why uses "selector.select(millis)", see "NioServerConnectionFactory#run()"
     */
    @Override
    public void doTransport(int waitTimeOut, List<Packet> pendingPacketQueue, LinkedList<Packet> outgoingPacketQueue, ClientConnection clientConnection) throws Exception {

        selector.select(waitTimeOut);
        Set<SelectionKey> selectedKeySet;
        synchronized (this) {
            selectedKeySet = selector.selectedKeys();
        }
        // Everything below and until we get back to the select is
        // non-blocking, so time is effectively a constant. That is
        // Why we just have to do this once, here
        updateNow();
        for (SelectionKey selectedKey : selectedKeySet) {
            SocketChannel socketChannel = ((SocketChannel) selectedKey.channel());
            if (selectedKey.isConnectable()) {
                if (socketChannel.finishConnect()) {
                    updateLastSendAndHeard();
                    sendThread.primeConnection();
                }
            } else if (selectedKey.isReadable() || selectedKey.isWritable()) {
                doIO(pendingPacketQueue, outgoingPacketQueue, clientConnection);
            }
        }
        if (sendThread.getClientConnectionState().isConnected()) {
            synchronized(outgoingPacketQueue) {
                if (findSendablePacket(outgoingPacketQueue) != null) {
                    enableWrite();
                }
            }
        }
        selectedKeySet.clear();
    }

    @Override
    public void connect(InetSocketAddress serverAddress) throws Exception {
        SocketChannel socketChannel = createSocketChannel();
        try {
            if (clientPort != null) {
                InetSocketAddress clientAddress = new InetSocketAddress(clientPort);
                socketChannel.socket().bind(clientAddress);
                LOG.info(LogUtils.getMessage("Client socket channel is bound to client address: {}"), clientAddress);
            } else {
                LOG.info(LogUtils.getMessage("Client socket channel is not bound to any client address, which means the host and port are auto-decided"));
            }
            registerAndConnect(socketChannel, serverAddress);
        } catch (IOException e) {
            LOG.error("Unable to open socket to " + serverAddress);
            socketChannel.close();
            throw e;
        }
        initialized = false;

        /*
         * Reset incomingBuffer
         */
        lenBuffer.clear();
        incomingBuffer = lenBuffer;
    }

    @Override
    public void cleanup() {
        if (registeredKey != null) {
            SocketChannel socketChannel = (SocketChannel) registeredKey.channel();
            registeredKey.cancel();
            try {
                socketChannel.socket().shutdownInput();
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring exception during shutdown input", e);
                }
            }
            try {
                socketChannel.socket().shutdownOutput();
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring exception during shutdown output",
                            e);
                }
            }
            try {
                socketChannel.socket().close();
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring exception during socket close", e);
                }
            }
            try {
                socketChannel.close();
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring exception during channel close", e);
                }
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("SendThread interrupted during sleep, ignoring");
            }
        }
        registeredKey = null;
    }

    @Override
    public void close() {
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Doing client selector close");
            }
            selector.close();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Closed client selector");
            }
        } catch (IOException e) {
            LOG.warn("Ignoring exception during selector close", e);
        }
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        // a lot could go wrong here, so rather than put in a bunch of code
        // to check for nulls all down the chain let's do it the simple
        // yet bulletproof way
        try {
            return ((SocketChannel) registeredKey.channel()).socket().getRemoteSocketAddress();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to create a socket channel
     *
     * @return
     * @throws Exception
     */
    private SocketChannel createSocketChannel() throws Exception {
        SocketChannel socketChannel = SocketChannel.open();
        LOG.info(LogUtils.getMessage("Client socket channel is opened"));
        socketChannel.configureBlocking(false);
        LOG.info(LogUtils.getMessage("Client socket channel is non-blocking: {}"), true);
        socketChannel.socket().setSoLinger(false, -1);
        socketChannel.socket().setTcpNoDelay(true);
        return socketChannel;
    }

    /**
     * This method is used to register the socket channel and connect it to the server
     *
     * @param socketChannel
     * @param serverAddress
     * @throws Exception
     */
    private void registerAndConnect(SocketChannel socketChannel, InetSocketAddress serverAddress) throws Exception {
        registeredKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        // information logging here
        LOG.info(LogUtils.getMessage("Client socket channel is registered with selector: {}"), selector);
        Set<SelectionKey> keys = selector.keys();
        LOG.info(LogUtils.getMessage("Number of registration of a client socket channel with a selector: {}"), keys.size());
        // For the case "socketChannel.configureBlocking(false)", immediateConnect is always false
        boolean immediateConnect = socketChannel.connect(serverAddress);
        if (immediateConnect) {
            sendThread.primeConnection();
        }
    }

    /******************************* Packet Processing *******************************/

    /**
     * This method is used to process I/O through connected socket channel
     * around {@link #incomingBuffer} or packets. <br/>
     * For processing read event, the data responded by the server is read into the
     * {@link #incomingBuffer} instance maintained by this socket through the connected
     * socket channel. Then according to the data in the buffer or if the client connection
     * is initialized (signified by {@link #initialized}), different operation will be performed
     * on the buffer (e.g., {@link #reallocateIncomingBuffer()}, {@link #readConnectResult()},
     * {@link ClientConnection.SendThread#readResponse(ByteBuffer)}). <br/>
     * For processing write event, the packet that needs to be sent to the server is
     * found by {@link #findSendablePacket(LinkedList)}.
     * Then the buffer (created by {@link Packet#createByteBuffer()}) of this packet will be
     * sent to the server through the connected socket channel. Then this packet will be
     * added from the end of {@link ClientConnection#pendingPacketQueue}, which means
     * this packet is waiting for the response from the server.
     * To remove this packet from {@link ClientConnection#pendingPacketQueue},
     * see {@link ClientConnection.SendThread#readResponse}.
     * [Process]
     *
     * @return true if a packet was received
     * @throws InterruptedException
     * @throws IOException
     */
    public void doIO(List<Packet> pendingPacketQueue, LinkedList<Packet> outgoingPacketQueue, ClientConnection clientConnection) throws Exception {
        SocketChannel connectedSocketChannel = (SocketChannel) registeredKey.channel();
        if (connectedSocketChannel == null) {
            throw new IOException("Socket is null!");
        }
        if (registeredKey.isReadable()) {
            int rc = connectedSocketChannel.read(incomingBuffer);
            if (rc < 0) {
                throw new EndOfStreamException("Unable to read additional data from server sessionid 0x" + Long.toHexString(sessionId) + ", likely server has closed socket");
            }
            if (!incomingBuffer.hasRemaining()) {
                incomingBuffer.flip();
                if (incomingBuffer == lenBuffer) {
                    // todo v.y. stats
//                    recvCount++;
                    reallocateIncomingBuffer();
                } else if (!initialized) {
                    readConnectResult();
                    enableRead();
                    if (findSendablePacket(outgoingPacketQueue) != null) {
                        // Since SASL authentication has completed (if client is configured to do so),
                        // outgoing packets waiting in the outgoingQueue can now be sent.
                        enableWrite();
                    }
                    lenBuffer.clear();
                    incomingBuffer = lenBuffer;
                    updateLastHeard();
                    initialized = true;
                } else {
                    sendThread.readResponse(incomingBuffer);
                    lenBuffer.clear();
                    incomingBuffer = lenBuffer;
                    updateLastHeard();
                }
            }
        }
        if (registeredKey.isWritable()) {
            synchronized(outgoingPacketQueue) {
                Packet p = findSendablePacket(outgoingPacketQueue);

                if (p != null) {
                    updateLastSend();
                    // If we already started writing p, p.bb will already exist
                    if (p.getByteBuffer() == null) {
                        if ((p.getRequestHeader() != null) && (p.getRequestHeader().getType() != OpCode.ping)) {
                            // original : p.requestHeader.setXid(serverConnection.getXid());
                            p.getRequestHeader().setXid(1);
                        }
                        p.createByteBuffer();
                    }
                    connectedSocketChannel.write(p.getByteBuffer());
                    if (!p.getByteBuffer().hasRemaining()) {
                        // todo v.y. stats
//                        sentCount++;
                        outgoingPacketQueue.removeFirstOccurrence(p);
                        if (p.getRequestHeader() != null && p.getRequestHeader().getType() != OpCode.ping) {
                            synchronized (pendingPacketQueue) {
                                pendingPacketQueue.add(p);
                            }
                        }
                    }
                }
                if (outgoingPacketQueue.isEmpty()) {
                    // No more packets to send: turn off write interest flag.
                    // Will be turned on later by a later call to enableWrite(),
                    // from within ZooKeeperSaslClient (if client is configured
                    // to attempt SASL authentication), or in either doIO() or
                    // in doTransport() if not.
                    disableWrite();
                } else if (!initialized && p != null && !p.getByteBuffer().hasRemaining()) {
                    // On initial connection, write the complete connect request
                    // packet, but then disable further writes until after
                    // receiving a successful connection response.  If the
                    // session is expired, then the server sends the expiration
                    // response and immediately closes its end of the socket.  If
                    // the client is simultaneously writing on its end, then the
                    // TCP stack may choose to abort with RST, in which case the
                    // client would never receive the session expired event.  See
                    // http://docs.oracle.com/javase/6/docs/technotes/guides/net/articles/connection_release.html
                    disableWrite();
                } else {
                    // Just in case
                    enableWrite();
                }
            }
        }
    }

    /**
     * This method is used to find a sendable packet. <br/>
     * In this method, the packet that needs to be sent to the server will be
     * removed from {@link ClientConnection#outgoingPacketQueue}.
     * [Process]
     *
     * @param outgoingPacketQueue
     * @return
     */
    public Packet findSendablePacket(LinkedList<Packet> outgoingPacketQueue) {
        synchronized (outgoingPacketQueue) {
            if (outgoingPacketQueue.isEmpty()) {
                return null;
            }
            // If we've already starting sending the first packet, we better finish
            if (outgoingPacketQueue.getFirst().getByteBuffer() != null) {
                return outgoingPacketQueue.getFirst();
            }

            // Since client's authentication with server is in progress,
            // send only the null-header packet queued by primeConnection().
            // This packet must be sent so that the SASL authentication process
            // can proceed, but all other packets should wait until
            // SASL authentication completes.
            ListIterator<Packet> iter = outgoingPacketQueue.listIterator();
            while (iter.hasNext()) {
                Packet p = iter.next();
                if (p.getRequestHeader() == null) {
                    // We've found the priming-packet. Move it to the beginning of the queue.
                    iter.remove();
                    outgoingPacketQueue.add(0, p);
                    return p;
                } else {
                    // Non-priming packet: defer it until later, leaving it in the queue
                    // until authentication completes.
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("deferring non-priming packet: " + p + "until SASL authentication completes.");
                    }
                }
            }
            // no sendable packet found.
            return null;
        }
    }
}
