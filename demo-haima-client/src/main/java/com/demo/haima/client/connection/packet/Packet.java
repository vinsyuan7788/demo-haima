package com.demo.haima.client.connection.packet;

import com.demo.haima.client.callback.asynchronous.AsyncCallback;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.request.ConnectRequest;
import com.demo.haima.common.serdes.jute.request.header.RequestHeader;
import com.demo.haima.common.serdes.jute.response.header.ResponseHeader;
import com.demo.haima.common.serdes.jute.utils.RecordUtils;
import com.demo.haima.common.utility.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class allows us to pass the headers and the relevant records around.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class Packet {

    private static final Logger LOG = LoggerFactory.getLogger(Packet.class);

    private RequestHeader requestHeader;
    private ResponseHeader responseHeader;
    private Record request;
    private Record response;
    private boolean readOnly;

    private boolean finished;

    /**
     * Client's view of the path (may differ due to chroot)
     */
    private String clientPath;
    /**
     * Servers's view of the path (may differ due to chroot)
     */
    private String serverPath;
    /**
     * Callback instance
     */
    private AsyncCallback callback;
    /**
     * Context object
     */
    private Object context;

    private ByteBuffer byteBuffer;

    public Packet(RequestHeader requestHeader, ResponseHeader responseHeader, Record request, Record response) {
        this(requestHeader, responseHeader, request, response, false);
    }

    public Packet(RequestHeader requestHeader, ResponseHeader responseHeader, Record request, Record response, boolean readOnly) {
        this.requestHeader = requestHeader;
        this.responseHeader = responseHeader;
        this.request = request;
        this.response = response;
        this.readOnly = readOnly;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "requestHeader=" + requestHeader +
                ", responseHeader=" + responseHeader +
                ", request=" + request +
                ", response=" + response +
                ", readOnly=" + readOnly +
                ", finished=" + finished +
                ", clientPath='" + clientPath + '\'' +
                ", serverPath='" + serverPath + '\'' +
                ", callback=" + callback +
                ", context=" + context +
                ", byteBuffer=" + byteBuffer +
                '}';
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to create a byte buffer. <br/>
     * In this method, the data in {@link RequestHeader} instance
     * and {@link Record}-typed request (e.g., {@link ConnectRequest})
     * instance is serialized to a {@link ByteArrayOutputStream}
     * instance, which then will be converted to a corresponding byte-array
     * and put into the {@link ByteBuffer} instance maintained by this packet.
     * [Process]
     */
    public void createByteBuffer() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
            // We'll fill this in later
            boa.writeInt(-1, "len");
            if (requestHeader != null) {
                requestHeader.serialize(boa, "header");
            }
            if (request instanceof ConnectRequest) {
                request.serialize(boa, "connect");
                // append "am-I-allowed-to-be-readonly" flag
                boa.writeBool(readOnly, "readOnly");
            } else if (request != null) {
                request.serialize(boa, "request");
            }
            // Information logging here
            LOG.info(LogUtils.getMessage("[Data]", "Request header sent to server: {}"), RecordUtils.toString(requestHeader));
            LOG.info(LogUtils.getMessage("[Data]", "Request sent to server: {}"), RecordUtils.toString(request));
            baos.close();
            this.byteBuffer = ByteBuffer.wrap(baos.toByteArray());
            this.byteBuffer.putInt(this.byteBuffer.capacity() - 4);
            this.byteBuffer.rewind();
        } catch (IOException e) {
            LOG.warn("Ignoring unexpected exception", e);
        }
    }

    /******************************* Getter and Setter *******************************/

    public RequestHeader getRequestHeader() {
        return requestHeader;
    }

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public Record getRequest() {
        return request;
    }

    public Record getResponse() {
        return response;
    }

    public String getClientPath() {
        return clientPath;
    }

    public AsyncCallback getCallback() {
        return callback;
    }

    public Object getContext() {
        return context;
    }

    public boolean getFinished() {
        return finished;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setClientPath(String clientPath) {
        this.clientPath = clientPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public void setCallback(AsyncCallback callback) {
        this.callback = callback;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
