package com.demo.haima.common.serdes.jute.iterator;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class BinaryIndex implements Index {

    private int nelems;

    public BinaryIndex(int nelems) {
        this.nelems = nelems;
    }

    @Override
    public boolean done() {
        return (nelems <= 0);
    }

    @Override
    public void incr() {
        nelems--;
    }
}
