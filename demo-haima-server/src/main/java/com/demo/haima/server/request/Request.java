package com.demo.haima.server.request;

import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.server.connection.ServerConnection;
import com.demo.haima.server.database.transaction.header.TxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * This is the structure that represents a request moving through a chain of
 * RequestProcessors. There are various pieces of information that is tacked
 * onto the request as it is processed.
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class Request {

    private static final Logger LOG = LoggerFactory.getLogger(Request.class);

    public static final Request REQUEST_OF_DEATH = new Request(null, 0, 0, null);

    /**
     * The wrapped client connection on the server-side
     */
    private final ServerConnection serverConnection;
    /**
     * The session ID from client connection
     */
    private final long sessionId;
    /**
     * The operation code from client connection
     */
    private final int type;
    /**
     * The buffer which stores the data from client connection
     */
    private final ByteBuffer incomingBuffer;

    private TxnHeader transactionHeader;

    private Record transaction;

    /**
     * The exception encountered when processing current request
     */
    private Exception exception;

    public Request(ServerConnection serverConnection, long sessionId, int type, ByteBuffer incomingBuffer) {
        this.serverConnection = serverConnection;
        this.sessionId = sessionId;
        this.type = type;
        this.incomingBuffer = incomingBuffer;
    }

    /**
     * This method is used to tell if the packet type is a valid packet in Haima
     *
     * @param type the type of the packet
     * @return true if a valid packet, false if not
     */
    public static boolean isValid(int type) {
        // make sure this is always synchronized with Zoodefs!!
        switch (type) {
//            case OpCode.notification:
//                return false;
            case OpCode.create:
            case OpCode.delete:
            case OpCode.createSession:
            case OpCode.exists:
            case OpCode.snowFlake:
            case OpCode.getData:
            case OpCode.check:
            case OpCode.multi:
            case OpCode.setData:
            case OpCode.sync:
            case OpCode.getChildren:
            case OpCode.getChildren2:
            case OpCode.ping:
            case OpCode.closeSession:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "Request{" +
                "serverConnection=" + serverConnection +
                ", sessionId=" + sessionId +
                ", type=" + type +
                ", incomingBuffer=" + incomingBuffer +
                ", transactionHeader=" + transactionHeader +
                ", transaction=" + transaction +
                ", exception=" + exception +
                '}';
    }

    /****************************** Getter and Setter ******************************/

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    public long getSessionId() {
        return sessionId;
    }

    public int getType() {
        return type;
    }

    public ByteBuffer getIncomingBuffer() {
        return incomingBuffer;
    }

    public TxnHeader getTransactionHeader() {
        return transactionHeader;
    }

    public void setTransactionHeader(TxnHeader transactionHeader) {
        this.transactionHeader = transactionHeader;
    }

    public Record getTransaction() {
        return transaction;
    }

    public void setTransaction(Record transaction) {
        this.transaction = transaction;

    }
    public Exception getException() {
        return this.exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
