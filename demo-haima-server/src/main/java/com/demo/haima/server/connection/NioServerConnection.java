package com.demo.haima.server.connection;

import com.demo.haima.common.build.Version;
import com.demo.haima.common.runtime.Environment;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.response.header.ResponseHeader;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.utils.RecordUtils;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.connection.factory.NioServerConnectionFactory;
import com.demo.haima.server.connection.factory.ServerConnectionFactory;
import com.demo.haima.server.exception.CloseRequestException;
import com.demo.haima.server.exception.EndOfStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class handles communication with clients using NIO. There is one per
 * client, but only one thread doing the communication
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class NioServerConnection extends ServerConnection {

    private static final Logger LOG = LoggerFactory.getLogger(NioServerConnection.class);
    private static final byte[] FOUR_BYTES_BUFFER = new byte[4];

    private HaimaServer server;
    private SocketChannel acceptedChannel;
    private SelectionKey registeredKey;
    private NioServerConnectionFactory serverConnectionFactory;

    /**
     *  A flag that signifies if the connection is initialized.
     *  Its default value is false (which is the default value of boolean-typed variable as well)
     */
    private boolean initialized;
    private ByteBuffer lenBuffer = ByteBuffer.allocate(4);
    /**
     * A buffer into which the data will be read from accepted channel
     */
    private ByteBuffer incomingBuffer = lenBuffer;

    /**
     * The buffers that need to be sent to the client
     */
    public LinkedBlockingQueue<ByteBuffer> outgoingBufferQueue = new LinkedBlockingQueue<>();
    /**
     * This is the id that uniquely identifies the session of a client. Once
     * this session is no longer active, the ephemeral nodes will go away.
     */
    private long sessionId;
    /**
     * This is the timeout for the session of the client with {@link NioServerConnection#sessionId}
     */
    private int sessionTimeout;

    public NioServerConnection(HaimaServer server, SocketChannel acceptedChannel, SelectionKey registeredKey, NioServerConnectionFactory serverConnectionFactory) throws Exception {
        this.server = server;
        LOG.info(LogUtils.getMessage("Server is set"));
        this.acceptedChannel = acceptedChannel;
        LOG.info(LogUtils.getMessage("Accepted Socket Channel is set"));
        this.registeredKey = registeredKey;
        LOG.info(LogUtils.getMessage("Registered selection key is set"));
        this.serverConnectionFactory = serverConnectionFactory;
        LOG.info(LogUtils.getMessage("Server connection factory is set"));
        // todo v.y. stats
//        if (server != null) {
//            outstandingLimit = server.getGlobalOutstandingLimit();
//        }
        acceptedChannel.socket().setTcpNoDelay(true);
        /* set socket linger to false, so that socket close does not
         * block */
        acceptedChannel.socket().setSoLinger(false, -1);
        // todo v.y. unknown
//        InetAddress addr = ((InetSocketAddress) socketChannel.socket()
//                .getRemoteSocketAddress()).getAddress();
        registeredKey.interestOps(SelectionKey.OP_READ);
    }

    public SocketChannel getSocketChannel() {
        return acceptedChannel;
    }

    /**
     * This method is used to process I/O through accepted socket channel
     * around {@link #incomingBuffer} or the buffer assigned from
     * {@link NioServerConnectionFactory#getDirectBuffer()}. <br/>
     * For processing read event, the data received from the client is read
     * into the {@link #incomingBuffer} instance maintained by this connection
     * through the accepted socket channel. Then according to the data in the
     * buffer, different operation will be performed evetuanlly on the buffer
     * (e.g., {@link #reallocateIncomingBuffer(SelectionKey)}, {@link #readConnectRequest()},
     * {@link #readRequest()}). <br/>
     * For processing write event, the byte buffer that needs to be sent to the client is found
     * from {@link #outgoingBufferQueue}. Then the byte buffer will be put into a direct buffer
     * (see {@link NioServerConnectionFactory#getDirectBuffer()}) for fast I/O. Then the direct
     * buffer will be written to the client through the accepted socket channel.
     * To add the byte buffer that needs to be sent to the client from the end of {@link #incomingBuffer},
     * see {@link #internalSendBuffer(ByteBuffer)}.
     * [Process]
     *
     * @param selectedKey
     */
    public void doIO(SelectionKey selectedKey) {
        try {
            if (!isSocketOpen()) {
                LOG.warn("trying to do i/o on a null socket for session:0x"
                        + Long.toHexString(sessionId));
                return;
            }
            if (selectedKey.isReadable()) {
                int readCount = acceptedChannel.read(incomingBuffer);
                if (readCount < 0) {
                    throw new EndOfStreamException(
                            "Unable to read additional data from client sessionid 0x"
                                    + Long.toHexString(sessionId)
                                    + ", likely client has closed socket");
                }
                if (incomingBuffer.remaining() == 0) {
                    boolean isPayload;
                    // start of next request
                    if (incomingBuffer == lenBuffer) {
                        incomingBuffer.flip();
                        isPayload = reallocateIncomingBuffer(selectedKey);
                        incomingBuffer.clear();
                    } else {
                        // continuation
                        isPayload = true;
                    }

                    // v.y. print for data
//                    System.out.println("doIO | isPayload: " + isPayload);

                    // not the case for 4-letter word
                    if (isPayload) {
                        readPayload();
                    }
                    else {
                        // four letter words take care
                        // need not do anything else
                        return;
                    }
                }
            }
            if (selectedKey.isWritable()) {
                // ZooLog.logTraceMessage(LOG,
                // ZooLog.CLIENT_DATA_PACKET_TRACE_MASK
                // "outgoingBufferQueue.size() = " +
                // outgoingBufferQueue.size());
                if (outgoingBufferQueue.size() > 0) {
                    // ZooLog.logTraceMessage(LOG,
                    // ZooLog.CLIENT_DATA_PACKET_TRACE_MASK,
                    // "sk " + k + " is valid: " +
                    // k.isValid());

                    /*
                     * This is going to reset the buffer position to 0 and the
                     * limit to the size of the buffer, so that we can fill it
                     * with data from the non-direct buffers that we need to
                     * send.
                     */
                    ByteBuffer directBuffer = serverConnectionFactory.getDirectBuffer();
                    directBuffer.clear();

                    for (ByteBuffer b : outgoingBufferQueue) {
                        if (directBuffer.remaining() < b.remaining()) {
                            /*
                             * When we call put later, if the directBuffer is to
                             * small to hold everything, nothing will be copied,
                             * so we've got to slice the buffer if it's too big.
                             */
                            b = (ByteBuffer) b.slice().limit(directBuffer.remaining());
                        }
                        /*
                         * put() is going to modify the positions of both
                         * buffers, put we don't want to change the position of
                         * the source buffers (we'll do that after the send, if
                         * needed), so we save and reset the position after the
                         * copy
                         */
                        int p = b.position();
                        directBuffer.put(b);
                        b.position(p);
                        if (directBuffer.remaining() == 0) {
                            break;
                        }
                    }
                    /*
                     * Do the flip: limit becomes position, position gets set to
                     * 0. This sets us up for the write.
                     */
                    directBuffer.flip();

                    int sent = acceptedChannel.write(directBuffer);
                    ByteBuffer bb;

                    // Remove the buffers that we have sent
                    while (outgoingBufferQueue.size() > 0) {
                        bb = outgoingBufferQueue.peek();
                        if (bb == ServerConnectionFactory.CLOSE_CONNECTION_BUFFER) {
                            // from the NioServerConnection.sendBuffer(ServerConnectionFactory.CLOSE_CONNECTION_BUFFER)
                            // that will trigger this throw exception and in the "catch clause" ---see the catch block
                            // we will close the connection and remove it from the factory connections list
                            throw new CloseRequestException("close requested");
                        }
                        int left = bb.remaining() - sent;
                        if (left > 0) {
                            /*
                             * We only partially sent this buffer, so we update
                             * the position and exit the loop.
                             */
                            bb.position(bb.position() + sent);
                            break;
                        }
                        // todo v.y. stats
//                        packetSent();
                        /* We've sent the whole buffer, so drop the buffer */
                        sent -= bb.remaining();
                        outgoingBufferQueue.remove();
                    }
                }

                synchronized(this.serverConnectionFactory){
                    if (outgoingBufferQueue.size() == 0) {
                        if (!initialized && (registeredKey.interestOps() & SelectionKey.OP_READ) == 0) {
                            throw new CloseRequestException("responded to info probe");
                        }
                        // Disable write event
                        registeredKey.interestOps(registeredKey.interestOps() & (~SelectionKey.OP_WRITE));
                    } else {
                        // Enable write event
                        registeredKey.interestOps(registeredKey.interestOps() | SelectionKey.OP_WRITE);
                    }
                }
            }
        } catch (Exception e) {
            close();
        }
    }

    @Override
    public void close() {
        synchronized(serverConnectionFactory.getClientConnectionSet()){
            // if this is not in clientConnectionSet then it's already closed
            if (!serverConnectionFactory.getClientConnectionSet().remove(this)) {
                return;
            }

            synchronized (serverConnectionFactory.getClientIpAddressAndConnectionsMap()) {
                Set<NioServerConnection> clientConnections = serverConnectionFactory.getClientIpAddressAndConnectionsMap().get(acceptedChannel.socket().getInetAddress());
                clientConnections.remove(this);
            }

            serverConnectionFactory.unregisterConnection(this);

            closeAcceptedChannelAndSocket();

            if (registeredKey != null) {
                try {
                    // need to cancel this selection key from the selector
                    registeredKey.cancel();
                } catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("ignoring exception during selectionkey cancel", e);
                    }
                }
            }
        }
    }

    @Override
    public void disableReceive() {
        registeredKey.interestOps(registeredKey.interestOps() & (~SelectionKey.OP_READ));
    }

    @Override
    public void enableRecv() {
        synchronized (this.serverConnectionFactory) {
            registeredKey.selector().wakeup();
            if (registeredKey.isValid()) {
                int interest = registeredKey.interestOps();
                if ((interest & SelectionKey.OP_READ) == 0) {
                    registeredKey.interestOps(interest | SelectionKey.OP_READ);
                }
            }
        }
    }

    @Override
    public void sendBuffer(ByteBuffer byteBuffer) {
        try {
            internalSendBuffer(byteBuffer);
        } catch(Exception e) {
            LOG.error("Unexpected Exception: ", e);
        }
    }

    /**
     * In this method, the {@link ResponseHeader} and {@link Record}-typed response
     * instance () that contains necessary information will be serialized and written
     * to a {@link ByteArrayOutputStream} instance, whose byte array format will be
     * wrapped to a byte buffer that needs to be sent to the client. Then this byte
     * buffer will be added from the end of {@link #outgoingBufferQueue}.
     * To add the byte buffer from the end of {@link #outgoingBufferQueue},
     * see {@link #internalSendBuffer(ByteBuffer)}.
     *
     * @param responseHeader response header
     * @param record {@link Record}-typed response
     * @param tag
     */
    @Override
    public void sendResponse(ResponseHeader responseHeader, Record record, String tag) {
        try {
            // information logging here
            LOG.info(LogUtils.getMessage("[Data]", "Response header sent to client: {}"), RecordUtils.toString(responseHeader));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Make space for length
            BinaryOutputArchive bos = BinaryOutputArchive.getArchive(baos);
            try {
                baos.write(FOUR_BYTES_BUFFER);
                bos.writeRecord(responseHeader, "header");
                if (record != null) {
                    // information logging here
                    LOG.info(LogUtils.getMessage("[Data]", "Response sent to client: {}"), RecordUtils.toString(record));
                    bos.writeRecord(record, tag);
                }
                baos.close();
            } catch (IOException e) {
                LOG.error("Error serializing response");
            }
            byte[] b = baos.toByteArray();
            ByteBuffer bb = ByteBuffer.wrap(b);
            bb.putInt(b.length - 4).rewind();
            sendBuffer(bb);
            // todo v.y. stats
//            synchronized(this){
//                outstandingRequests--;
//            }
            // check throttling
            synchronized (this.serverConnectionFactory) {
                // todo v.y. stats
//                if (server.getNumberOfRequestInProcess() < outstandingLimit || outstandingRequests < 1) {
                if (server.getNumberOfRequestInProcess() < 1000) {
                    registeredKey.selector().wakeup();
                    enableRecv();
                }
            }
        } catch(Exception e) {
            LOG.warn("Unexpected exception. Destruction averted.", e);
        }
    }

    @Override
    public void sendCloseSession() {
        // Send close connection packet to the client, doIO will eventually
        // close the underlying machinery (like socket, selectorkey, etc...)
        sendBuffer(ServerConnectionFactory.CLOSE_CONNECTION_BUFFER);
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        if (acceptedChannel.isOpen() == false) {
            return null;
        }
        return acceptedChannel.socket().getRemoteSocketAddress();
    }

    @Override
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * This method is used to close resources associated with the socket of this connection
     */
    private void closeAcceptedChannelAndSocket() {
        if (acceptedChannel.isOpen() == false) {
            LOG.info("Accepted socket channel is shutdown");
            return;
        }
        LOG.info("Closed socket connection for client "
                + acceptedChannel.socket().getRemoteSocketAddress()
                + (sessionId != 0 ?
                " which had sessionid 0x" + Long.toHexString(sessionId) :
                " (no session established for client)"));

        try {
            /*
             * The following sequence of code is stupid! You would think that
             * only sock.close() is needed, but alas, it doesn't work that way.
             * If you just do sock.close() there are cases where the socket
             * doesn't actually close...
             */
            acceptedChannel.socket().shutdownOutput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            if (LOG.isDebugEnabled()) {
                LOG.debug("ignoring exception during output shutdown", e);
            }
        }
        try {
            acceptedChannel.socket().shutdownInput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            if (LOG.isDebugEnabled()) {
                LOG.debug("ignoring exception during input shutdown", e);
            }
        }
        try {
            acceptedChannel.socket().close();
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ignoring exception during socket close", e);
            }
        }
        try {
            acceptedChannel.close();
            // XXX The next line doesn't seem to be needed, but some posts
            // to forums suggest that it is needed. Keep in mind if errors in
            // this section arise.
            // factory.selector.wakeup();
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ignoring exception during socketchannel close", e);
            }
        }
        LOG.info("Accepted socket channel is shutdown");
    }

    /**
     *  This method is used to tell if the accepted socket is open
     *
     * @return
     */
    private boolean isSocketOpen() {
        return acceptedChannel.isOpen();
    }

    /**
     * Reads the first 4 bytes of lenBuffer, which could be true length or
     * four letter word.
     *
     * @param selectedKey the selected selection key
     * @return true if length read, otw false (wasn't really the length)
     * @throws IOException if buffer size exceeds maxBuffer size
     */
    private boolean reallocateIncomingBuffer(SelectionKey selectedKey) throws Exception {
        // Read the length, now get the buffer
        int len = lenBuffer.getInt();
        if (!initialized && checkFourLetterWord(selectedKey, len)) {
            return false;
        }
        if (len < 0 || len > BinaryInputArchive.MAX_BUFFER) {
            throw new IOException("Len error " + len);
        }
        if (!isServerRunning()) {
            throw new IOException("Server not running");
        }
        incomingBuffer = ByteBuffer.allocate(len);
        return true;
    }

    /**
     * This method is used to return if four letter word found and responded to, otherwise false
     *
     * @param selectedKey
     * @param bufferLength
     * @return
     * @throws IOException
     */
    private boolean checkFourLetterWord(final SelectionKey selectedKey, final int bufferLength) throws Exception {

        // We take advantage of the limited size of the length to look
        // for cmds. They are all 4-bytes which fits inside of an int
        if (!ServerConnection.isKnown(bufferLength)) {
            return false;
        }

        // todo v.y. stats
//        packetReceived();

        /** cancel the selection key to remove the socket handling
         * from selector. This is to prevent netcat problem wherein
         * netcat immediately closes the sending side after sending the
         * commands and still keeps the receiving channel open.
         * The idea is to remove the selectionkey from the selector
         * so that the selector does not notice the closed read on the
         * socket channel and keep the socket alive to write the data to
         * and makes sure to close the socket after its done writing the data
         */
        if (selectedKey != null) {
            try {
                selectedKey.cancel();
            } catch(Exception e) {
                LOG.error("Error cancelling command selection key ", e);
            }
        }

        final PrintWriter pwriter = new PrintWriter(
                new BufferedWriter(new SendBufferWriter()));

        String cmd = ServerConnection.getCommandString(bufferLength);
        // ZOOKEEPER-2693: don't execute 4lw if it's not enabled.
        if (!ServerConnection.isEnabled(cmd)) {
            LOG.debug("Command {} is not executed because it is not in the whitelist.", cmd);
            NopCommand nopCmd = new NopCommand(pwriter, cmd + " is not executed because it is not in the whitelist.");
            nopCmd.start();
            return true;
        }

        LOG.info("Processing " + cmd + " command from "
                + acceptedChannel.socket().getRemoteSocketAddress());

        if (bufferLength == ruokCmd) {
            RuokCommand ruok = new RuokCommand(pwriter);
            ruok.start();
            return true;
        } else if (bufferLength == getTraceMaskCmd) {
            TraceMaskCommand tmask = new TraceMaskCommand(pwriter);
            tmask.start();
            return true;
        } else if (bufferLength == setTraceMaskCmd) {
            incomingBuffer = ByteBuffer.allocate(8);
            int rc = acceptedChannel.read(incomingBuffer);
            if (rc < 0) {
                throw new IOException("Read error");
            }

            incomingBuffer.flip();
            long traceMask = incomingBuffer.getLong();
            // todo v.y. trace
//            BETrace.setTextTraceLevel(traceMask);
            SetTraceMaskCommand setMask = new SetTraceMaskCommand(pwriter, traceMask);
            setMask.start();
            return true;
        } else if (bufferLength == enviCmd) {
            EnvCommand env = new EnvCommand(pwriter);
            env.start();
            return true;
        } else if (bufferLength == confCmd) {
            ConfCommand ccmd = new ConfCommand(pwriter);
            ccmd.start();
            return true;
        } else if (bufferLength == srstCmd) {
            StatResetCommand strst = new StatResetCommand(pwriter);
            strst.start();
            return true;
        } else if (bufferLength == crstCmd) {
            CnxnStatResetCommand crst = new CnxnStatResetCommand(pwriter);
            crst.start();
            return true;
        } else if (bufferLength == dumpCmd) {
            DumpCommand dump = new DumpCommand(pwriter);
            dump.start();
            return true;
        } else if (bufferLength == statCmd || bufferLength == srvrCmd) {
            StatCommand stat = new StatCommand(pwriter, bufferLength);
            stat.start();
            return true;
        } else if (bufferLength == consCmd) {
            ConsCommand cons = new ConsCommand(pwriter);
            cons.start();
            return true;
        } else if (bufferLength == mntrCmd) {
            MonitorCommand mntr = new MonitorCommand(pwriter);
            mntr.start();
            return true;
        } else if (bufferLength == isroCmd) {
            IsroCommand isro = new IsroCommand(pwriter);
            isro.start();
            return true;
        }
        return false;
    }

    /**
     * Read the request payload (everything following the length prefix)
     *
     * @throws Exception
     */
    private void readPayload() throws Exception {
        // have we read length bytes?
        if (incomingBuffer.remaining() != 0) {
            // socket is non-blocking, so ok
            int rc = acceptedChannel.read(incomingBuffer);
            if (rc < 0) {
                throw new EndOfStreamException(
                        "Unable to read additional data from client sessionid 0x"
                                + Long.toHexString(sessionId)
                                + ", likely client has closed socket");
            }
        }

        // have we read length bytes?
        if (incomingBuffer.remaining() == 0) {
            // todo v.y. stats
//            packetReceived();
            incomingBuffer.flip();

            // v.y. print for data
//            System.out.println("readPayload | initialized: " + initialized);

            if (!initialized) {
                readConnectRequest();
            } else {
                readRequest();
            }
            lenBuffer.clear();
            incomingBuffer = lenBuffer;
        }
    }

    private void readConnectRequest() throws Exception {
        if (!isServerRunning()) {
            throw new IOException("Server not running");
        }
        server.processConnectRequest(this, incomingBuffer);
        initialized = true;
    }

    private void readRequest() throws Exception {
        server.processPacket(this, incomingBuffer);
    }

    public void internalSendBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer != ServerConnectionFactory.CLOSE_CONNECTION_BUFFER) {
            // We check if write interest here because if it is NOT set,
            // nothing is queued, so we can try to send the buffer right
            // away without waking up the selector
            if(registeredKey.isValid() && ((registeredKey.interestOps() & SelectionKey.OP_WRITE) == 0)) {
                try {
                    acceptedChannel.write(byteBuffer);
                } catch (IOException e) {
                    // we are just doing best effort right now
                }
            }
            // if there is nothing left to send, we are done
            if (byteBuffer.remaining() == 0) {
                // todo v.y. stats
//                packetSent();
                return;
            }
        }

        synchronized(this.serverConnectionFactory){
            registeredKey.selector().wakeup();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Add a buffer to outgoingBufferQueue, registered selection key " + registeredKey + " is valid: " + registeredKey.isValid());
            }
            outgoingBufferQueue.add(byteBuffer);
            if (registeredKey.isValid()) {
                registeredKey.interestOps(registeredKey.interestOps() | SelectionKey.OP_WRITE);
            }
        }
    }

    private boolean isServerRunning() {
        return server != null && server.isRunning();
    }

    /******************************* Class Definition *******************************/

    /**
     * This class wraps the sendBuffer method of NIOServerConnection. It is
     * responsible for chunking up the response to a client. Rather
     * than cons'ing up a response fully in memory, which may be large
     * for some commands, this class chunks up the result.
     */
    private class SendBufferWriter extends Writer {
        private StringBuffer sb = new StringBuffer();

        /**
         * Check if we are ready to send another chunk.
         * @param force force sending, even if not a full chunk
         */
        private void checkFlush(boolean force) {
            if ((force && sb.length() > 0) || sb.length() > 2048) {
                sendBufferSync(ByteBuffer.wrap(sb.toString().getBytes()));
                // clear our internal buffer
                sb.setLength(0);
            }
        }

        @Override
        public void close() throws IOException {
            if (sb == null) {
                return;
            }
            checkFlush(true);
            sb = null; // clear out the ref to ensure no reuse
        }

        @Override
        public void flush() throws IOException {
            checkFlush(true);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            sb.append(cbuf, off, len);
            checkFlush(false);
        }

        /**
         * send buffer without using the asynchronous
         * calls to selector and then close the socket
         * @param bb
         */
        private void sendBufferSync(ByteBuffer bb) {
            try {
                /* configure socket to be blocking
                 * so that we dont have to do write in
                 * a tight while loop
                 */
                acceptedChannel.configureBlocking(true);
                if (bb != ServerConnectionFactory.CLOSE_CONNECTION_BUFFER) {
                    if (acceptedChannel.isOpen()) {
                        acceptedChannel.write(bb);
                    }
                    // todo v.y. stats
//                    packetSent();
                }
            } catch (IOException ie) {
                LOG.error("Error sending data synchronously ", ie);
            }
        }
    }

    private static final String MESSAGE_OF_SERVER_NOT_RUNNING = "This Server instance is not currently serving requests";
    
    /**
     * Set of threads for command ports. All the 4
     * letter commands are run via a thread. Each class
     * maps to a corresponding 4 letter command. CommandThread
     * is the abstract class from which all the others inherit.
     */
    private abstract class CommandThread extends Thread {
        PrintWriter pw;

        CommandThread(PrintWriter pw) {
            this.pw = pw;
        }

        @Override
        public void run() {
            try {
                LOG.info(LogUtils.getMessage("Command thread {} starts running"), getClass().getSimpleName());
                commandRun();
                LOG.info(LogUtils.getMessage("Command thread {} completes running"), getClass().getSimpleName());
            } catch (IOException ie) {
                LOG.error("Error in running command ", ie);
            } finally {
                cleanupWriterSocket(pw);
            }
        }

        /**
         * clean up the socket related to a command and also make sure we flush the
         * data before we do that
         *
         * @param pwriter the pwriter for a command socket
         */
        private void cleanupWriterSocket(PrintWriter pwriter) {
            try {
                if (pwriter != null) {
                    pwriter.flush();
                    pwriter.close();
                }
            } catch (Exception e) {
                LOG.info("Error closing PrintWriter ", e);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    LOG.error("Error closing a command socket ", e);
                }
            }
        }

        public abstract void commandRun() throws IOException;
    }

    private class RuokCommand extends CommandThread {
        public RuokCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            pw.print("imok");

        }
    }

    private class TraceMaskCommand extends CommandThread {
        TraceMaskCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            // todo v.y. trace
//            long traceMask = BETrace.getTextTraceLevel();
//            pw.print(traceMask);
        }
    }

    private class SetTraceMaskCommand extends CommandThread {
        long trace = 0;
        SetTraceMaskCommand(PrintWriter pw, long trace) {
            super(pw);
            this.trace = trace;
        }

        @Override
        public void commandRun() {
            pw.print(trace);
        }
    }

    private class EnvCommand extends CommandThread {
        EnvCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            List<Environment.Entry> env = Environment.list();

            pw.println("Environment:");
            for(Environment.Entry e : env) {
                pw.print(e.getKey());
                pw.print("=");
                pw.println(e.getValue());
            }

        }
    }

    private class ConfCommand extends CommandThread {
        ConfCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if (!isServerRunning()) {
                pw.println(MESSAGE_OF_SERVER_NOT_RUNNING);
            } else {
                // todo v.y. conf
//                server.dumpConf(pw);
            }
        }
    }

    private class StatResetCommand extends CommandThread {
        public StatResetCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if (!isServerRunning()) {
                pw.println(MESSAGE_OF_SERVER_NOT_RUNNING);
            }
            else {
                // todo v.y. stats
//                server.serverStats().reset();
                pw.println("Server stats reset.");
            }
        }
    }

    private class CnxnStatResetCommand extends CommandThread {
        public CnxnStatResetCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if (!isServerRunning()) {
                pw.println(MESSAGE_OF_SERVER_NOT_RUNNING);
            } else {
                synchronized(serverConnectionFactory.getClientConnectionSet()){
                    for(ServerConnection cnxn : serverConnectionFactory.getClientConnectionSet()){
                        // todo v.y. stats
//                        cnxn.resetStats();
                    }
                }
                pw.println("Connection stats reset.");
            }
        }
    }

    private class DumpCommand extends CommandThread {
        public DumpCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if (!isServerRunning()) {
                pw.println(MESSAGE_OF_SERVER_NOT_RUNNING);
            }
            else {
                pw.println("SessionTracker dump:");
                // todo v.y. session
//                server.sessionTracker.dumpSessions(pw);
            }
        }
    }

    private class StatCommand extends CommandThread {
        int len;
        public StatCommand(PrintWriter pw, int len) {
            super(pw);
            this.len = len;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void commandRun() {
            if (!isServerRunning()) {
                pw.println(MESSAGE_OF_SERVER_NOT_RUNNING);
            }
            else {
                pw.print("Haima version: ");
                pw.println(Version.getFullVersion());
                // todo v.y. read-only server
//                if (beServer instanceof ReadOnlyHaimaServer) {
//                    pw.println("READ-ONLY mode; serving only " +
//                            "read-only clients");
//                }
                if (len == statCmd) {
                    LOG.info("Stat command output");
                    pw.println("Clients:");
                    // clone should be faster than iteration
                    // i.e., give up the clientConnectionSet lock faster
                    HashSet<NioServerConnection> clientConnectionSet;
                    synchronized(serverConnectionFactory.getClientConnectionSet()){
                        clientConnectionSet = (HashSet<NioServerConnection>) serverConnectionFactory.getClientConnectionSet().clone();
                    }
                    for(NioServerConnection cnxn : clientConnectionSet){
                        // todo v.y. stats
//                        cnxn.dumpConnectionInfo(pw, true);
                        pw.println();
                    }
                    pw.println();
                }
                // todo v.y. stats
//                pw.print(server.serverStats().toString());

                pw.print("Node count: ");
                // todo v.y. db-stats
//                pw.println(server.getBEDatabase().getNodeCount());
            }

        }
    }

    private class ConsCommand extends CommandThread {
        public ConsCommand(PrintWriter pw) {
            super(pw);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void commandRun() {
            if (!isServerRunning()) {
                pw.println(MESSAGE_OF_SERVER_NOT_RUNNING);
            } else {
                // clone should be faster than iteration
                // ie give up the clientConnectionSet lock faster
                HashSet<NioServerConnection> clientConnectionSet;
                synchronized (serverConnectionFactory.getClientConnectionSet()) {
                    clientConnectionSet = (HashSet<NioServerConnection>) serverConnectionFactory.getClientConnectionSet().clone();
                }
                for (NioServerConnection cnxn : clientConnectionSet) {
                    // todo v.y. stats
//                    cnxn.dumpConnectionInfo(pw, false);
                    pw.println();
                }
                pw.println();
            }
        }
    }

    private class MonitorCommand extends CommandThread {

        MonitorCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if(!isServerRunning()) {
                pw.println(MESSAGE_OF_SERVER_NOT_RUNNING);
                return;
            }

            print("version", Version.getFullVersion());

            // todo v.y. stats
//            ServerStats stats = server.serverStats();
//            print("avg_latency", stats.getAvgLatency());
//            print("max_latency", stats.getMaxLatency());
//            print("min_latency", stats.getMinLatency());
//
//            print("packets_received", stats.getPacketsReceived());
//            print("packets_sent", stats.getPacketsSent());
//            print("num_alive_connections", stats.getNumAliveClientConnections());
//
//            print("outstanding_requests", stats.getOutstandingRequests());

            // todo v.y. db-stats
//            BEDatabase beDb = server.getBEDatabase();
//            print("server_state", stats.getServerState());
//            print("znode_count", beDb.getNodeCount());
//            print("approximate_data_size", beDb.getDataTree().approximateDataSize());

//            OSMXBean osMbean = new OSMXBean();
//            if (osMbean != null && osMbean.getUnix() == true) {
//                print("open_file_descriptor_count", osMbean.getOpenFileDescriptorCount());
//                print("max_file_descriptor_count", osMbean.getMaxFileDescriptorCount());
//            }
        }

        private void print(String key, long number) {
            print(key, "" + number);
        }

        private void print(String key, String value) {
            pw.print("zk_");
            pw.print(key);
            pw.print("\t");
            pw.println(value);
        }

    }

    private class IsroCommand extends CommandThread {

        public IsroCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if (!isServerRunning()) {
                pw.print("null");
                // todo v.y. read-only server
//            } else if (server instanceof ReadOnlyHaimaServer) {
//                pw.print("ro");
            } else {
                pw.print("rw");
            }
        }
    }

    private class NopCommand extends CommandThread {
        private String msg;

        public NopCommand(PrintWriter pw, String msg) {
            super(pw);
            this.msg = msg;
        }

        @Override
        public void commandRun() {
            pw.println(msg);
        }
    }
}
