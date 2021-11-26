package com.demo.haima.server.request.processor;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.exception.RequestProcessorException;
import com.demo.haima.server.exception.exiter.UncaughtExceptionExiter;
import com.demo.haima.server.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Flushable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.demo.haima.server.request.Request.REQUEST_OF_DEATH;

/**
 * This RequestProcessor logs requests to MySQL. It batches the requests to do
 * the io efficiently. The request is not passed to the next RequestProcessor
 * until its log has been synced to MySQL.
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class RequestSynchronousProcessor extends UncaughtExceptionExiter implements RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSynchronousProcessor.class);

    private HaimaServer server;
    private RequestProcessor nextRequestProcessor;

    /**
     * A flag that siginifies if this processor thread is running
     */
    private volatile boolean running;

    private final BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
    /**
     * Transactions that have been written and are waiting to be flushed to
     * disk. Basically this is the list of SyncItems whose callbacks will be
     * invoked after flush returns successfully.
     */
    private final LinkedList<Request> requestListToFlush = new LinkedList<>();

    public RequestSynchronousProcessor(HaimaServer server, RequestProcessor nextRequestProcessor) {
        super("SyncThread:" + server.getServerId(), server.getServerListener());
        this.server = server;
        LOG.info(LogUtils.getMessage("Server is set"));
        this.nextRequestProcessor = nextRequestProcessor;
        LOG.info(LogUtils.getMessage("Next request processor thread is set"));
    }

    @Override
    public void run() {
        LOG.info(LogUtils.getMessage("Request synchronous processor thread starts running"));
        running = true;
        try {
            int logCount = 0;

            while (true) {
                Request si = null;
                if (requestListToFlush.isEmpty()) {
                    si = requestQueue.take();
                } else {
                    si = requestQueue.poll();
                    if (si == null) {
                        flush(requestListToFlush);
                        continue;
                    }
                }
                if (si == REQUEST_OF_DEATH) {
                    break;
                }
                if (si != null) {
                    // track the number of records written to the log
                    if (si.getTransactionHeader() != null) {
                        logCount++;
                        //TODO if he hdr is not null, means this is a change request and write it to MySQL
                    } else if (requestListToFlush.isEmpty()) {
                        // optimization for read heavy workloads
                        // if this is a read, and there are no pending
                        // flushes (writes), then just pass this to the next
                        // processor
                        if (nextRequestProcessor != null) {
                            nextRequestProcessor.processRequest(si);
                            if (nextRequestProcessor instanceof Flushable) {
                                ((Flushable) nextRequestProcessor).flush();
                            }
                        }
                        continue;
                    }
                    requestListToFlush.add(si);
                    if (requestListToFlush.size() > 1000) {
                        flush(requestListToFlush);
                    }
                }
            }
        } catch (Throwable t) {
            handleException(this.getName(), t);
            running = false;
        }
        LOG.info(LogUtils.getMessage("Request synchronous processor thread completes running"));
    }

    @Override
    public void processRequest(Request request) {
        // request.addRQRec(">sync");
        requestQueue.add(request);
    }

    @Override
    public void shutdown() {
        requestQueue.add(REQUEST_OF_DEATH);
        try {
            if(running){
                this.join();
            }
            if (!requestListToFlush.isEmpty()) {
                flush(requestListToFlush);
            }
        } catch(Exception e) {
            if (e instanceof InterruptedException) {
                LOG.warn("Interrupted while wating for " + this + " to finish");
            } else if (e instanceof IOException) {
                LOG.warn("Got IO exception during shutdown");
            } else if (e instanceof RequestProcessorException) {
                LOG.warn("Got request processor exception during shutdown");
            }
        }
        if (nextRequestProcessor != null) {
            nextRequestProcessor.shutdown();
        }
        LOG.info(LogUtils.getMessage("Request synchronous processor is shut down"));
    }

    /**
     * This method is used to flush the requests if necessary (e.g., when the number of requests reaches threshold, etc.)
     *
     * @param requestListToFlush
     * @throws Exception
     */
    private void flush(LinkedList<Request> requestListToFlush) throws Exception {
        if (requestListToFlush.isEmpty()) {
            return;
        }
        //bes.getBEDatabase().commit();
        while (!requestListToFlush.isEmpty()) {
            Request i = requestListToFlush.remove();
            if (nextRequestProcessor != null) {
                nextRequestProcessor.processRequest(i);
            }
        }
        if (nextRequestProcessor != null && nextRequestProcessor instanceof Flushable) {
            ((Flushable) nextRequestProcessor).flush();
        }
    }
}
