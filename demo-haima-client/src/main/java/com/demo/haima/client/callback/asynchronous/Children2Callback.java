package com.demo.haima.client.callback.asynchronous;

import com.demo.haima.common.serdes.jute.statistics.Stat;

import java.util.List;

/**
 * This callback is used to retrieve the children and stat of the node.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public interface Children2Callback extends AsyncCallback {

    /**
     * Process the result of the asynchronous call.
     * See {@link ChildrenCallback}.
     *
     * @param rc       The return code or the result of the call.
     * @param path     The path that we passed to asynchronous calls.
     * @param ctx      Whatever context object that we passed to
     *                 asynchronous calls.
     * @param children An unordered array of children of the node on
     *                 given path.
     * @param stat     {@link Stat} object of
     *                 the node on given path.
     */
    void processResult(int rc, String path, Object ctx, List<String> children, Stat stat);
}