package com.demo.haima.test.unit.buffer;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.test.unit.buffer.bytes.ByteBufferBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.Buffer;

/**
 * @author Vince Yuan
 * @date 2021/11/17
 */
public class BufferBase {

    private static final Logger LOG = LoggerFactory.getLogger(ByteBufferBase.class);

    protected void logBufferInfo(Buffer buffer) {
        // Mark: the index to which its position will be reset when reset() is invoked.
        // It is not always defined, but when it is defined, it is never negative and is never greater than position
        // -- For ByteBuffer, its default value is -1 (i.e., undefined) can be changed by mark() only,

        // Offset: the offset of the first element of the buffer within this buffer's backing array
        // If this buffer is backed by an array, then buffer position p corresponds to array index p + arrayOffset().
        // -- For ByteBuffer, its default value is 0 once a byte buffer is instantiated, and it remains unchanged
        LOG.info(LogUtils.getMessage("logBufferInfo", "Offset: {}"), buffer.arrayOffset());
        // Position: the index of the next element to be read and written
        // -- For ByteBuffer, its default value is 0 once a byte buffer is instantiated
        // it is never negative and is never greater than limit
        // Relative operations read or write one or more elements starting at the current position and then increment the position by the number of elements transferred
        // -- For ByteBuffer, they are put(), get() or getXXX() (position will be incremented by 1, 4 or 8, etc. depending on which method is invoked)
        // -- For ReadableByteChannel and WritableByteChannel, read(buffer) and write(buffer) will always invoke relative operations
        // Absolute operations take an explicit element index and do not affect the position.
        // -- For ByteBuffer, they are put(index, data), get(index), getXXX(index) method.
        LOG.info(LogUtils.getMessage("logBufferInfo", "Position: {}"), buffer.position());
        // Limit: the index of the first element that should NOT be read and written
        // -- It means (limit - 1) is the maximum index of element that can be read and written
        // it is never negative and is never greater than capacity
        // -- For ByteBuffer, It equals to the capacity by default once a byte buffer is instantiated
        LOG.info(LogUtils.getMessage("logBufferInfo", "Limit: {}"), buffer.limit());
        // Capacity: the number of elements the buffer contains
        // It is never negative and never changes
        // -- For ByteBuffer, it equals to the length of its backing array once a byte buffer is instantiated
        LOG.info(LogUtils.getMessage("logBufferInfo", "Capacity: {}"), buffer.capacity());
    }
}
