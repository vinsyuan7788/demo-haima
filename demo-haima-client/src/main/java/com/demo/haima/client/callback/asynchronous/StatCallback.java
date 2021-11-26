package com.demo.haima.client.callback.asynchronous;

import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.serdes.jute.statistics.Stat;

/**
 * This callback is used to retrieve the stat of the node.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public interface StatCallback extends AsyncCallback {

    /**
     * Process the result of the asynchronous call.
     * <p/>
     * On success, rc is
     * {@link HaimaException.Code#OK}.
     * <p/>
     * On failure, rc is set to the corresponding failure code in
     * {@link HaimaException}.
     * <ul>
     * <li>
     * {@link HaimaException.Code#NONODE}
     * - The node on given path doesn't exist for some API calls.
     * </li>
     * <li>
     * {@link HaimaException.Code#BADVERSION}
     * - The given version doesn't match the node's version
     * for some API calls.
     * </li>
     * </ul>
     *
     * @param rc   The return code or the result of the call.
     * @param path The path that we passed to asynchronous calls.
     * @param ctx  Whatever context object that we passed to
     *             asynchronous calls.
     * @param stat {@link Stat} object of
     *             the node on given path.
     */
    void processResult(int rc, String path, Object ctx, Stat stat);
}
