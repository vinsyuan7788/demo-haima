package com.demo.haima.test.integration.demo.utils.data.network.request.header;

import com.demo.haima.test.integration.demo.utils.data.network.connection.ConnectionUtils;
import com.demo.haima.test.integration.demo.utils.data.network.io.serdes.Record;
import com.demo.haima.test.integration.demo.utils.data.network.io.wrapper.InputWrapper;
import com.demo.haima.test.integration.demo.utils.data.network.io.wrapper.OutputWrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public class RequestHeader implements Record {

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

    private RequestHeader() { }

    private RequestHeader(int operationType) {
        this.connectionId = ConnectionUtils.getUuidString();
        this.operationType = operationType;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public int getOperationType() {
        return operationType;
    }

    public static RequestHeader create() {
        return new RequestHeader();
    }

    public static RequestHeader create(int operationType) {
        return new RequestHeader(operationType);
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
        return "RequestHeader{" +
                "connectionId=" + connectionId +
                ", operationType=" + operationType +
                '}';
    }
}
