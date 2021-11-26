package com.demo.haima.client.callback.asynchronous;

import com.demo.haima.client.callback.synchronous.SyncCallback;

/**
 * Interface definitions of asynchronous callbacks.
 * An asynchronous callback is deferred to invoke after a function returns.
 * Asynchronous calls usually improve system efficiency on IO-related APIs.
 * <p/>
 * Haima provides asynchronous version as equivalent to synchronous APIs.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public interface AsyncCallback extends SyncCallback {
}
