package com.demo.haima.common.serdes.jute.iterator;

/**
 * Interface that acts as an iterator for deserializing maps.
 * The deserializer returns an instance that the record uses to
 * read vectors and maps. An example of usage is as follows:
 *
 * <code>
 * Index idx = startVector(...);
 * while (!idx.done()) {
 *   .... // read element of a vector
 *   idx.incr();
 * }
 * </code>
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface Index {

    boolean done();
    void incr();
}
