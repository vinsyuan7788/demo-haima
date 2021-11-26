package com.demo.haima.server.request.processor;

import com.demo.haima.server.request.Request;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public interface RequestProcessor {

    /**
     * This method is used to process the submitted {@link Request} instance.
     * [Process]
     *
     * @param request
     * @throws Exception
     */
    void processRequest(Request request) throws Exception;

    /**
     * This method is used to shut down current request processor
     */
    void shutdown();
}
