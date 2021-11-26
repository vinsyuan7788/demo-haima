package com.demo.haima.server.request.processor;

import com.demo.haima.common.definition.ReplyCode;
import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.serdes.jute.response.header.ResponseHeader;
import com.demo.haima.server.exception.RequestProcessorException;
import com.demo.haima.server.request.Request;

/**
 * Manages the unknown requests (i.e. unknown OpCode), by:
 * - sending back the KeeperException.UnimplementedException() error code to the client
 * - closing the connection.
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class RequestUnimplementedProcessor implements RequestProcessor {

    @Override
    public void processRequest(Request request) throws Exception {
        HaimaException hmex = new HaimaException.UnimplementedException();
        request.setException(hmex);
        ResponseHeader rh = new ResponseHeader(ReplyCode.exceptionReply, hmex.code().intValue());
        try {
            request.getServerConnection().sendResponse(rh, null, "response");
        } catch (Exception e) {
            throw new RequestProcessorException("Can't send the response", e);
        }

        request.getServerConnection().sendCloseSession();
    }

    @Override
    public void shutdown() {
        // todo v.y.
    }
}
