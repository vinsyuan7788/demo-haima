package com.demo.haima.test.unit.buffer.bytes;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.test.unit.buffer.BufferBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vince Yuan
 * @date 2021/11/17
 */
public abstract class ByteBufferBase extends BufferBase {

    private static final Logger LOG = LoggerFactory.getLogger(ByteBufferBase.class);

    protected ByteBuffer byteBuffer1;
    protected ByteBuffer byteBuffer2;

    private final List<Integer> markPositionList = Arrays.asList(1, 4, 7);
    private int exceptionCount = 0;

    protected void testCreateHeapByteBuffer() {

        System.out.println("testCreateByteBuffer starts");

        // Create a heap byte buffer
        byteBuffer1 = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        System.out.println("Byte buffer 1:");
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        // Create another heap byte buffer
        byteBuffer2 = ByteBuffer.allocate(16);
        System.out.println("Byte buffer 2:");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        System.out.println("testCreateByteBuffer completes");
    }

    protected void testCreateDirectByteBuffer() {

        System.out.println("testCreateDirectByteBuffer starts");
        System.out.println("testCreateDirectByteBuffer completes");
    }

    /**
     * This method is used to test reading data to the byte buffer <br/>
     * For relative operations: get() and getXXX() will increment the position of the buffer. <br/>
     * For absolute operations: get(index) and getXXX(index) will NOT increment the position of the buffer
     */
    protected void testByteBufferGet() {

        System.out.println("testByteBufferGet starts");

        // Get the byte on the position:
        // This will increment the position
        byte getByte = byteBuffer1.get();
        System.out.println("byte on the position from byte buffer 1: " + getByte);
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        // Get a byte (as int) at specific index:
        // This will NOT increment the position
        int getInt = byteBuffer1.get(3);
        System.out.println("byte at specific index from byte buffer 1: " + getInt);
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        // Get the next 4 bytes (starting from current position) and compose them (in a fixed way) into an integer value:
        // This will increment the position
        // How to compose: @see java.io.Bits#makeInt(byte, byte, byte, byte)
        int int1 = byteBuffer1.getInt();
        System.out.println("integer on the position from byte buffer 1: " + int1);
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        // Get the next 4 bytes (starting from specific index) and compose them (in a fixed way) into an integer value:
        // This will NOT increment the position
        // How to compose: @see java.io.Bits#makeInt(byte, byte, byte, byte)
        int int2 = byteBuffer1.getInt(4);
        System.out.println("integer at specific index from byte buffer 1: " + int2);
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        // If two buffer contains the same bytes, then getInt(), getLong(), etc. will be equal
        testGetIntEqual();

        System.out.println("testByteBufferGet completes");
    }

    /**
     * This method is used to test writing data to the byte buffer <br/>
     * For relative operations: put() will increment the position of the buffer. <br/>
     * For absolute operations: put(index, data) will NOT increment the position of the buffer
     */
    protected void testByteBufferPut() {

        System.out.println("testByteBufferPut starts");

        // Put a byte on the position:
        // This will increment the position
        byteBuffer2.put((byte) 1);
        System.out.println("After putting a byte on the position, byte buffer 2:");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        // Put a byte at specific index:
        // This will NOT increment the position
        byteBuffer2.put(2, (byte) 3);;
        System.out.println("After putting a byte at specific position, byte buffer 2:");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        // Put a byte array on the position:
        // This will increment the position
        byteBuffer2.position(3);
        byteBuffer2.put(new byte[] { 4, 5, 6 });
        System.out.println("After putting a byte array on the position, byte buffer 2:");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        System.out.println("testByteBufferPut completes");
    }

    protected void testByteBufferFlip() {

        System.out.println("testByteBufferFlip starts");

        System.out.println("Before flipping the byte buffer 2:");
        logDataToProcess(byteBuffer2, "Data that can be read (or (over-)written if desired)");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        byteBuffer2.flip();

        System.out.println("After flipping the byte buffer 2:");
        logDataToProcess(byteBuffer2, "Data that can be read (or (over-)written if desired)");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        System.out.println("testByteBufferFlip completes");
    }

    protected void testByteBufferClear() {

        System.out.println("testByteBufferClear starts");

        System.out.println("Reading data from the byte buffer 2...");
        logDataReadFromByteBuffer(byteBuffer2);

        System.out.println("Before clearing the byte buffer 2:");
        logDataToProcess(byteBuffer2, "Data that can be (over-)written");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        byteBuffer2.clear();

        System.out.println("After clearing the byte buffer 2:");
        logDataToProcess(byteBuffer2, "Data that can be (over-)written");
        logDataInByteBuffer(byteBuffer2);
        logBufferInfo(byteBuffer2);

        System.out.println("testByteBufferClear completes");
    }

    protected void testByteBufferRewind() {

        System.out.println("testByteBufferRewind starts");

        System.out.println("Before rewinding the byte buffer 1:");
        logDataToProcess(byteBuffer1, "Data that can be (re-)read");
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        byteBuffer1.rewind();

        System.out.println("After rewinding the byte buffer 1:");
        logDataToProcess(byteBuffer1, "Data that can be (re-)read");
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        System.out.println("testByteBufferRewind completes");
    }

    protected void testByteBufferMarkAndReset() {

        System.out.println("testByteBufferMarkAndReset starts");

        while (byteBuffer1.hasRemaining()) {
            int position = byteBuffer1.position();
            if (markPositionList.contains(position)) {
                byteBuffer1.mark();
                processDataWithReset(byteBuffer1);
            } else {
                processDataNormally(byteBuffer1);
            }
        }

        System.out.println("testByteBufferMarkAndReset completes");
    }

    protected void testByteBufferSliceAndDuplicate() {

        System.out.println("testByteBufferSliceAndDuplicate starts");

        System.out.println("Information of the byte buffer 1:");
        logDataToProcess(byteBuffer1, "Data to process");
        logDataInByteBuffer(byteBuffer1);
        logBufferInfo(byteBuffer1);

        ByteBuffer sliceBuffer = byteBuffer1.slice();
        System.out.println("Slice buffer from the byte buffer 1:");
        logDataToProcess(sliceBuffer, "Data to process");
        logDataInByteBuffer(sliceBuffer);
        logBufferInfo(sliceBuffer);

        ByteBuffer duplicateBuffer = byteBuffer1.duplicate();
        System.out.println("Duplicate buffer from the byte buffer 1:");
        logDataToProcess(duplicateBuffer, "Data to process");
        logDataInByteBuffer(duplicateBuffer);
        logBufferInfo(duplicateBuffer);

        System.out.println("testByteBufferSliceAndDuplicate starts");
    }

    /**************************************** Scenario Simulation ****************************************/

    private void testGetIntEqual() {
        String data = "Hello Byte buffer";
        ByteBuffer bufferSent = ByteBuffer.wrap(data.getBytes());
        ByteBuffer bufferReceived = bufferSent.duplicate();
        System.out.println("if getInt() of two buffers containing the same data is equal: " + (bufferSent.getInt() == bufferReceived.getInt()));
    }

    private void processDataWithReset(ByteBuffer byteBuffer) {

        System.out.println("Before reading data from the byte buffer:");
        logDataToProcess(byteBuffer, "Data to process:");
        logDataInByteBuffer(byteBuffer);
        logBufferInfo(byteBuffer);

        int dataRead = byteBuffer.getInt();

        System.out.println("After reading data from the byte buffer:");
        if (exceptionCount < 1) {
            // Simulating data processing encounters exception, hence reset the byte buffer
            System.err.println("Data processing " + dataRead + " encounters exception, " +
                    "reset the byte buffer and process the data in this position again");
            exceptionCount++;
            byteBuffer.reset();
        } else {
            // Simulating data processing succeeds
            System.out.println("Data processing " + dataRead + " succeeds");
        }
        logDataToProcess(byteBuffer, "Data to process:");
        logDataInByteBuffer(byteBuffer);
        logBufferInfo(byteBuffer);
    }

    private void processDataNormally(ByteBuffer byteBuffer) {

        System.out.println("Before reading data from the byte buffer:");
        logDataToProcess(byteBuffer, "Data to process:");
        logDataInByteBuffer(byteBuffer);
        logBufferInfo(byteBuffer);

        int dataRead = byteBuffer.getInt();

        System.out.println("After reading data from the byte buffer:");
        // Simulating data processing succeeds
        System.out.println("Data processing " + dataRead + " succeeds");
        logDataToProcess(byteBuffer, "Data to process:");
        logDataInByteBuffer(byteBuffer);
        logBufferInfo(byteBuffer);
    }

    /**************************************** Data processing ****************************************/

    private synchronized String getDataInByteBuffer(ByteBuffer byteBuffer) {
        StringBuffer data = new StringBuffer();
        byte[] array = byteBuffer.array();
        if (array != null) {
            int length = array.length;
            for (int i = 0; i < length; i++) {
                data.append(array[i]);
                if (i < length - 1) {
                    data.append(",");
                }
            }
        }
        return data.toString();
    }

    private synchronized String getDataToProcess(ByteBuffer byteBuffer) {
        StringBuffer data = new StringBuffer();
        byte[] array = byteBuffer.array();
        if (array != null) {
            int limit = byteBuffer.limit();
            for (int i = byteBuffer.position(); i < limit; i++) {
                data.append(array[i]);
                if (i < limit - 1) {
                    data.append(",");
                }
            }
        }
        return data.toString();
    }

    private synchronized String getDataReadFromByteBuffer(ByteBuffer byteBuffer) {
        StringBuffer data = new StringBuffer();
        int limit = byteBuffer.limit();
        for (int i = byteBuffer.position(); i < limit; i = byteBuffer.position()) {
            data.append(byteBuffer.get());
            if (i < limit - 1) {
                data.append(",");
            }
        }
        return data.toString();
    }

    private synchronized String getDataWrittenIntoByteBuffer(ByteBuffer byteBuffer, byte[] dataToWrite) {
        StringBuffer data = new StringBuffer();
        if (dataToWrite != null) {
            int length = dataToWrite.length;
            for (int i = 0; i < length; i++) {
                if (byteBuffer.position() < byteBuffer.limit()) {
                    byteBuffer.put(dataToWrite[i]);
                    data.append(dataToWrite[i]);
                    if (i < length - 1) {
                        data.append(",");
                    }
                }
            }
        }
        return data.toString();
    }

    /**************************************** Log Method ****************************************/

    private void logDataInByteBuffer(ByteBuffer byteBuffer) {
        LOG.info(LogUtils.getMessage("logDataInByteBuffer", "Data in byte buffer: [{}]"), getDataInByteBuffer(byteBuffer));
    }

    private void logDataToProcess(ByteBuffer byteBuffer, String message) {
        LOG.info(LogUtils.getMessage("logDataToProcess", "{}: [{}]"), message, getDataToProcess(byteBuffer));
    }

    private void logDataReadFromByteBuffer(ByteBuffer byteBuffer) {
        LOG.info(LogUtils.getMessage("logDataReadFromByteBuffer", "Data read from byte buffer: [{}]"), getDataReadFromByteBuffer(byteBuffer));
    }

    private void logDataWrittenIntoByteBuffer(ByteBuffer byteBuffer, byte[] dataToWrite) {
        LOG.info(LogUtils.getMessage("logDataReadFromByteBuffer", "Data read from byte buffer: [{}]"), getDataWrittenIntoByteBuffer(byteBuffer, dataToWrite));
    }
}
