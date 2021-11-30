package com.demo.haima.fundamental.utility.data.network.response.header;

import com.demo.haima.fundamental.utility.data.network.io.serdes.Record;
import com.demo.haima.fundamental.utility.data.network.io.wrapper.InputWrapper;
import com.demo.haima.fundamental.utility.data.network.io.wrapper.OutputWrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public class ResponseHeader implements Record {

    /**
     * The ID that represents an entire connection (client -> server -> client)
     * In this connection, this ID will remain unchanged (which can be used to
     * keep track of the packet, etc.).
     * Notice that this ID is not necessarily globally unique, only to ensure
     * that this ID is unique on the local machine that establishes this connection.
     */
    private String connectionId;
    /**
     * The field that signifies what the operation is for this connection (e.g.,
     * to simply transmit a data, to upload a file, etc.).
     * This will decide what request and response body is
     */
    private int operationType;

    private ResponseHeader() { }

    private ResponseHeader(String connectionId, int operationType) {
        this.connectionId = connectionId;
        this.operationType = operationType;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public int getOperationType() {
        return operationType;
    }

    public static ResponseHeader create() {
        return new ResponseHeader();
    }

    public static ResponseHeader create(String connectionIdFromRequestHeader, int operationType) {
        return new ResponseHeader(connectionIdFromRequestHeader, operationType);
    }

    @Override
    public void serializeTo(OutputWrapper outputWrapper) {
        outputWrapper.writeString(connectionId);
        outputWrapper.writeInt(operationType);
    }

    @Override
    public void deserializeFrom(InputWrapper inputWrapper) {
        connectionId = inputWrapper.readString();
        operationType = inputWrapper.readInt();
    }

    @Override
    public String toString() {
        return "ResponseHeader{" +
                "connectionId=" + connectionId +
                ", operationType=" + operationType +
                '}';
    }
}
