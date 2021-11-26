package com.demo.haima.client.callback.asynchronous;

import com.demo.haima.common.exception.HaimaException;

/**
 * This callback is used to retrieve the name of the node.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public interface StringCallback extends AsyncCallback {

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
     * {@link HaimaException.Code#NODEEXISTS}
     * - The node on give path already exists for some API calls.
     * </li>
     * <li>
     * {@link HaimaException.Code#NONODE}
     * - The node on given path doesn't exist for some API calls.
     * </li>
     * </ul>
     *
     * @param rc   The return code or the result of the call.
     * @param path The path that we passed to asynchronous calls.
     * @param ctx  Whatever context object that we passed to
     *             asynchronous calls.
     * @param name The name of the node that was created.
     *             On success, <i>name</i> and <i>path</i> are usually
     *             equal, unless a sequential node has been created.
     */
    void processResult(int rc, String path, Object ctx, String name);
}