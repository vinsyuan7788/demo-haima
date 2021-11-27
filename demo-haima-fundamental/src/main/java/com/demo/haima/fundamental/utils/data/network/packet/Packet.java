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
    private ByteBuffer byteBuffer;
    private PacketProcessState packetProcessState;
    private long memoryAddress = VM.current().addressOf(this);

    public Packet(RequestHeader requestHeader, RequestBody requestBody, ByteBuffer byteBuffer) {
        this.requestHeader = requestHeader;
        this.requestBody = requestBody;
        this.byteBuffer = byteBuffer;
        setPacketProcessState(PacketProcessState.INITIAL);
    }

    public Packet(ResponseHeader responseHeader, ResponseBody responseBody, ByteBuffer byteBuffer) {
        this.responseHeader = responseHeader;
        this.responseBody = responseBody;
        this.byteBuffer = byteBuffer;
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

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /***************************** State Machine *****************************/

    public void setPacketProcessState(PacketProcessState packetProcessState) {
        this.packetProcessState = packetProcessState;
    }

    public PacketProcessState getPacketProcessState() {
        return packetProcessState;
    }

    /***************************** Utility Method *****************************/

    /**
     * This method is used to create a packet from the request header and body
     *
     * @param requestHeader
     * @param requestBody
     * @param byteBufferType
     * @return
     */
    public static Packet create(RequestHeader requestHeader, RequestBody requestBody, int byteBufferType) {

        BinaryOutputWrapper outputWrapper = BinaryOutputWrapper.create();

        requestHeader.serializeTo(outputWrapper);
        requestBody.serializeTo(outputWrapper);

        byte[] byteArray = outputWrapper.getByteArray();
        ByteBuffer byteBuffer = createByteBuffer(byteArray, byteBufferType);

        return new Packet(requestHeader, requestBody, byteBuffer);
    }

    /**
     * This method is used to create a packet from the response header and body
     *
     * @param responseHeader
     * @param responseBody
     * @param byteBufferType
     * @return
     */
    public static Packet create(ResponseHeader responseHeader, ResponseBody responseBody, int byteBufferType) {

        BinaryOutputWrapper outputWrapper = BinaryOutputWrapper.create();

        responseHeader.serializeTo(outputWrapper);
        responseBody.serializeTo(outputWrapper);

        byte[] byteArray = outputWrapper.getByteArray();
        ByteBuffer byteBuffer = createByteBuffer(byteArray, byteBufferType);

        return new Packet(responseHeader, responseBody, byteBuffer);
    }

    /**
     * This method is used to read the byte buffer received by server to packet
     *
     * @param byteBuffer
     * @return
     */
    public static Packet readOnServer(ByteBuffer byteBuffer) {

        byteBuffer.flip();
        BinaryInputWrapper inputWrapper = BinaryInputWrapper.create(byteBuffer);

        RequestHeader header = RequestHeader.create();
        header.deserializeFrom(inputWrapper);

        RequestBody body = createRequestBody(header);
        body.deserializeFrom(inputWrapper);

        return new Packet(header, body, byteBuffer);
    }

    /**
     * This method is used to read the byte buffer received by client to packet
     *
     * @param byteBuffer
     * @return
     */
    public static Packet readOnClient(ByteBuffer byteBuffer) {

        byteBuffer.flip();
        BinaryInputWrapper inputWrapper = BinaryInputWrapper.create(byteBuffer);

        ResponseHeader header = ResponseHeader.create();
        header.deserializeFrom(inputWrapper);

        ResponseBody body = createResponseBody(header);
        body.deserializeFrom(inputWrapper);

        return new Packet(header, body, byteBuffer);
    }

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

    @Override
    public String toString() {
        return "Packet{" +
                "requestHeader=" + requestHeader +
                ", requestBody=" + requestBody +
                ", responseHeader=" + responseHeader +
                ", responseBody=" + responseBody +
                ", byteBuffer=" + byteBuffer +
                ", packetProcessState=" + packetProcessState +
                ", memoryAddress=" + memoryAddress +
                '}';
    }

}
