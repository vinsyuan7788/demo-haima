package com.demo.haima.fundamental.utils.data.network.packet;

import com.demo.haima.fundamental.utils.data.network.definition.ByteBufferType;
import com.demo.haima.fundamental.utils.data.network.definition.OperationType;
import com.demo.haima.fundamental.utils.data.network.io.wrapper.BinaryInputWrapper;
import com.demo.haima.fundamental.utils.data.network.io.wrapper.BinaryOutputWrapper;
import com.demo.haima.fundamental.utils.data.network.request.body.RequestBody;
import com.demo.haima.fundamental.utils.data.network.request.body.SnowflakeIdRequestBody;
import com.demo.haima.fundamental.utils.data.network.request.body.TransmitDataRequestBody;
import com.demo.haima.fundamental.utils.data.network.request.header.RequestHeader;
import com.demo.haima.fundamental.utils.data.network.response.body.ResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.SnowflakeIdResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.TransmitDataResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.header.ResponseHeader;
import com.demo.haima.fundamental.utils.exception.DemoException;
import com.demo.haima.fundamental.utils.state.packet.PacketProcessState;
import org.openjdk.jol.vm.VM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public class Packet {

    private static final Logger LOG = LoggerFactory.getLogger(Packet.class);

    private RequestHeader requestHeader;
    private RequestBody requestBody;
    private ResponseHeader responseHeader;
    private ResponseBody responseBody;

    private PacketProcessState packetProcessState;
    private long memoryAddress = VM.current().addressOf(this);

    public Packet(RequestHeader requestHeader, RequestBody requestBody) {
        this.requestHeader = requestHeader;
        this.requestBody = requestBody;
        setPacketProcessState(PacketProcessState.INITIAL);
    }

    public Packet(ResponseHeader responseHeader, ResponseBody responseBody) {
        this.responseHeader = responseHeader;
        this.responseBody = responseBody;
        setPacketProcessState(PacketProcessState.INITIAL);
    }

    /***************************** Getter and Setter *****************************/

    public RequestHeader getRequestHeader() {
        return requestHeader;
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public void setResponseHeader(ResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseBody(ResponseBody responseBody) {
        this.responseBody = responseBody;
    }

    public ResponseBody getResponseBody() {
        return responseBody;
    }

    /***************************** State Machine *****************************/

    public void setPacketProcessState(PacketProcessState packetProcessState) {
        this.packetProcessState = packetProcessState;
    }

    public PacketProcessState getPacketProcessState() {
        return packetProcessState;
    }

    /***************************** Constructor Method *****************************/

    /**
     * This method is used to create a packet
     *
     * @param requestHeader
     * @param requestBody
     * @return
     */
    public static Packet create(RequestHeader requestHeader, RequestBody requestBody) {
        return new Packet(requestHeader, requestBody);
    }

    /**
     * This method is used to create a packet
     *
     * @param responseHeader
     * @param responseBody
     * @return
     */
    public static Packet create(ResponseHeader responseHeader, ResponseBody responseBody) {
        return new Packet(responseHeader, responseBody);
    }

    /***************************** Packet Method *****************************/

    /**
     * This method is used to create a byte buffer
     *
     * @param byteBufferType
     * @return
     */
    public ByteBuffer createByteBufferOnClient(int byteBufferType) {
        if (requestHeader == null || requestBody == null) {
            throw new DemoException("Request header or body is null");
        }

        BinaryOutputWrapper outputWrapper = BinaryOutputWrapper.create();
        requestHeader.serializeTo(outputWrapper);
        requestBody.serializeTo(outputWrapper);
        byte[] byteArrayOfBody = outputWrapper.getByteArray();
        return createByteBuffer(byteArrayOfBody, byteBufferType);
    }

    /**
     * This method is used to create a byte buffers
     *
     * @param byteBufferType
     * @return
     */
    public ByteBuffer createByteBufferOnServer(int byteBufferType) {
        if (responseHeader == null || responseBody == null) {
            throw new DemoException("Response header or body is null");
        }

        BinaryOutputWrapper outputWrapper = BinaryOutputWrapper.create();
        responseHeader.serializeTo(outputWrapper);
        responseBody.serializeTo(outputWrapper);
        byte[] byteArrayOfBody = outputWrapper.getByteArray();
        return createByteBuffer(byteArrayOfBody, byteBufferType);
    }

    /**
     * This method is used to read the byte buffer received by client to packet
     *
     * @param byteBuffer
     * @return
     */
    public static Packet readOnClient(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            throw new DemoException("Byte buffer is null");
        }

        byteBuffer.flip();
        BinaryInputWrapper inputWrapper = BinaryInputWrapper.create(byteBuffer);

        ResponseHeader header = ResponseHeader.create();
        header.deserializeFrom(inputWrapper);

        ResponseBody body = createResponseBody(header);
        body.deserializeFrom(inputWrapper);

        return new Packet(header, body);
    }

    /**
     * This method is used to read the byte buffer received by server to packet
     *
     * @param byteBuffer
     * @return
     */
    public static Packet readOnServer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            throw new DemoException("Byte buffer is null");
        }

        byteBuffer.flip();
        BinaryInputWrapper inputWrapper = BinaryInputWrapper.create(byteBuffer);

        RequestHeader header = RequestHeader.create();
        header.deserializeFrom(inputWrapper);

        RequestBody body = createRequestBody(header);
        body.deserializeFrom(inputWrapper);

        return new Packet(header, body);
    }

    /************************ Packet Method (for gather-write and scatter-read) ************************/

    /**
     * This method is used to create byte buffers
     *
     * @param byteBufferType
     * @return
     */
    public ByteBuffer[] createByteBuffersOnClient(int byteBufferType) {
        if (requestHeader == null || requestBody == null) {
            throw new DemoException("Request header or body is null");
        }

        BinaryOutputWrapper outputWrapper = BinaryOutputWrapper.create();
        requestHeader.serializeTo(outputWrapper);
        byte[] byteArrayOfHeader = outputWrapper.getByteArray();
        ByteBuffer byteBufferOfHeader = createByteBuffer(byteArrayOfHeader, byteBufferType);

        outputWrapper = BinaryOutputWrapper.create();
        requestBody.serializeTo(outputWrapper);
        byte[] byteArrayOfBody = outputWrapper.getByteArray();
        ByteBuffer byteBufferOfBody = createByteBuffer(byteArrayOfBody, byteBufferType);

        return new ByteBuffer[] { byteBufferOfHeader, byteBufferOfBody };
    }

    /**
     * This method is used to create byte buffers
     *
     * @param byteBufferType
     * @return
     */
    public ByteBuffer[] createByteBuffersOnServer(int byteBufferType) {
        if (responseHeader == null || responseBody == null) {
            throw new DemoException("Response header or body is null");
        }

        BinaryOutputWrapper outputWrapper = BinaryOutputWrapper.create();
        responseHeader.serializeTo(outputWrapper);
        byte[] byteArrayOfHeader = outputWrapper.getByteArray();
        ByteBuffer byteBufferOfHeader = createByteBuffer(byteArrayOfHeader, byteBufferType);

        outputWrapper = BinaryOutputWrapper.create();
        responseBody.serializeTo(outputWrapper);
        byte[] byteArrayOfBody = outputWrapper.getByteArray();
        ByteBuffer byteBufferOfBody = createByteBuffer(byteArrayOfBody, byteBufferType);

        return new ByteBuffer[] { byteBufferOfHeader, byteBufferOfBody };
    }

    /**
     * This method is used to read the data carried by byte buffers to a packet
     *
     * @param byteBufferOfHeader
     * @param byteBufferOfBody
     * @return
     */
    public static Packet readOnClient(ByteBuffer byteBufferOfHeader, ByteBuffer byteBufferOfBody) {
        if (byteBufferOfHeader == null || byteBufferOfBody == null) {
            throw new DemoException("Byte buffer of header or body is null");
        }

        byteBufferOfHeader.flip();
        BinaryInputWrapper inputWrapper = BinaryInputWrapper.create(byteBufferOfHeader);
        ResponseHeader header = ResponseHeader.create();
        header.deserializeFrom(inputWrapper);

        byteBufferOfBody.flip();
        inputWrapper = BinaryInputWrapper.create(byteBufferOfBody);
        ResponseBody body = createResponseBody(header);
        body.deserializeFrom(inputWrapper);

        return new Packet(header, body);
    }

    /**
     * This method is used to read the data carried by byte buffers to a packet
     *
     * @param byteBufferOfHeader
     * @param byteBufferOfBody
     * @return
     */
    public static Packet readOnServer(ByteBuffer byteBufferOfHeader, ByteBuffer byteBufferOfBody) {
        if (byteBufferOfHeader == null || byteBufferOfBody == null) {
            throw new DemoException("Byte buffer of header or body is null");
        }

        byteBufferOfHeader.flip();
        BinaryInputWrapper inputWrapper = BinaryInputWrapper.create(byteBufferOfHeader);
        RequestHeader header = RequestHeader.create();
        header.deserializeFrom(inputWrapper);

        byteBufferOfBody.flip();
        inputWrapper = BinaryInputWrapper.create(byteBufferOfBody);
        RequestBody body = createRequestBody(header);
        body.deserializeFrom(inputWrapper);

        return new Packet(header, body);
    }

    /***************************** Utility Method *****************************/

    /**
     * This method is sued to create a byte buffer
     *
     * @param byteArray
     * @param byteBufferType
     * @return
     */
    private static ByteBuffer createByteBuffer(byte[] byteArray, int byteBufferType) {
        ByteBuffer byteBuffer;
        switch (byteBufferType) {
            case ByteBufferType.HEAP:
                byteBuffer = (byteArray != null) ? ByteBuffer.wrap(byteArray) : ByteBuffer.allocate(0);
                break;
            case ByteBufferType.DIRECT:
                if (byteArray != null) {
                    byteBuffer = ByteBuffer.allocateDirect(byteArray.length);
                    byteBuffer.put(byteArray);
                    byteBuffer.flip();
                } else {
                    byteBuffer = ByteBuffer.allocateDirect(0);
                }
                break;
            default:
                throw new DemoException("Byte buffer type is unknown");
        }
        return byteBuffer;
    }

    /**
     * This method is used to create a request body
     *
     * @param header
     * @return
     */
    private static RequestBody createRequestBody(RequestHeader header) {
        RequestBody requestBody;
        int operationType = header.getOperationType();
        switch (operationType) {
            case OperationType.TRANSMIT_DATA:
                requestBody = TransmitDataRequestBody.create();
                break;
            case OperationType.SNOWFLAKE_ID:
                requestBody = SnowflakeIdRequestBody.create();
                break;
            default:
                throw new DemoException("Operation type is unknown: " + operationType
                        + " | header: " + header);
        }
        return requestBody;
    }

    /**
     * This method is used to create a response body
     *
     * @param header
     * @return
     */
    private static ResponseBody createResponseBody(ResponseHeader header) {
        ResponseBody responseBody;
        int operationType = header.getOperationType();
        switch (operationType) {
            case OperationType.TRANSMIT_DATA:
                responseBody = TransmitDataResponseBody.create();
                break;
            case OperationType.SNOWFLAKE_ID:
                responseBody = SnowflakeIdResponseBody.create();
                break;
            default:
                throw new DemoException("Operation type is unknown: " + operationType);
        }
        return responseBody;
    }

    /***************************** Overridden Method *****************************/

    @Override
    public String toString() {
        return "Packet{" +
                "requestHeader=" + requestHeader +
                ", requestBody=" + requestBody +
                ", responseHeader=" + responseHeader +
                ", responseBody=" + responseBody +
                ", packetProcessState=" + packetProcessState +
                ", memoryAddress=" + memoryAddress +
                '}';
    }
}
