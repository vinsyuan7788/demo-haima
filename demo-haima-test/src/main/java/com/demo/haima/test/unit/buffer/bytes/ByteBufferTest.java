package com.demo.haima.test.unit.buffer.bytes;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Vince Yuan
 * @date 2021/11/17
 */
public class ByteBufferTest extends ByteBufferBase {

    private static final String HEAP = "HEAP";
    private static final String DIRECT = "DIRECT";

    private final String bufferType = HEAP;

    @Before
    public void testCreateByteBuffer() {
        if (HEAP.equals(bufferType)) {
            testCreateHeapByteBuffer();
            System.out.println();
        } else if (DIRECT.equals(bufferType)) {
            testCreateDirectByteBuffer();
            System.out.println();
        } else {
            System.out.println("No need to test, hence exit normally.");
            System.exit(0);
        }
    }

    @Test
    public void testByteBufferOperation() {

        testByteBufferGet();
        System.out.println();

        testByteBufferPut();
        System.out.println();

        testByteBufferSliceAndDuplicate();
        System.out.println();

        testByteBufferRewind();
        System.out.println();

        testByteBufferFlip();
        System.out.println();

        testByteBufferClear();
        System.out.println();

        testByteBufferMarkAndReset();
        System.out.println();
    }
}
