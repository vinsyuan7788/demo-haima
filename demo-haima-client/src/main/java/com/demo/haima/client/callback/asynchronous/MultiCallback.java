package com.demo.haima.client.callback.asynchronous;

import com.demo.haima.common.definition.OpResult;
import com.demo.haima.common.exception.HaimaException;

import java.util.List;

/**
 * This callback is used to process the multiple results from
 * a single multi call.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public interface MultiCallback extends AsyncCallback {

    /**
     * Process the result of the asynchronous call.
     * <p/>
     * On success, rc is
     * {@link HaimaException.Code#OK}.
     * All opResults are
     * non-{@link OpResult.ErrorResult},
     *
     * <p/>
     * On failure, rc is a failure code in
     * {@link HaimaException.Code}.
     * All opResults are
     * {@link OpResult.ErrorResult}.
     * All operations will be rollback-ed even if operations
     * before the failing one were successful.
     *
     * @param rc   The return code or the result of the call.
     * @param path The path that we passed to asynchronous calls.
     * @param ctx  Whatever context object that we passed to
     *             asynchronous calls.
     * @param opResults The list of results.
     *                  One result for each operation,
     *                  and the order matches that of input.
     */
    void processResult(int rc, String path, Object ctx, List<OpResult> opResults);
}