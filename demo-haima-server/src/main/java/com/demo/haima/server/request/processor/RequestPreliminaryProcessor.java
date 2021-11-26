package com.demo.haima.server.request.processor;

import com.demo.haima.common.definition.CreateMode;
import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.io.ByteBufferInputStream;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.request.CheckVersionRequest;
import com.demo.haima.common.serdes.jute.request.CreateRequest;
import com.demo.haima.common.serdes.jute.request.DeleteRequest;
import com.demo.haima.common.serdes.jute.request.MultiTransactionRecord;
import com.demo.haima.common.serdes.jute.request.SetDataRequest;
import com.demo.haima.common.serdes.jute.request.operation.Op;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.common.utility.PathUtils;
import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.database.data.statistics.StatPersisted;
import com.demo.haima.server.database.data.structure.DataNode;
import com.demo.haima.server.database.transaction.CheckVersionTxn;
import com.demo.haima.server.database.transaction.CreateSessionTxn;
import com.demo.haima.server.database.transaction.CreateTxn;
import com.demo.haima.server.database.transaction.DeleteTxn;
import com.demo.haima.server.database.transaction.ErrorTxn;
import com.demo.haima.server.database.transaction.MultiTxn;
import com.demo.haima.server.database.transaction.SetDataTxn;
import com.demo.haima.server.database.transaction.Txn;
import com.demo.haima.server.database.transaction.header.TxnHeader;
import com.demo.haima.server.exception.exiter.UncaughtExceptionExiter;
import com.demo.haima.server.request.Request;
import com.demo.haima.server.request.processor.utils.ChangeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.demo.haima.server.request.Request.REQUEST_OF_DEATH;

/**
 * This request processor is generally at the start of a RequestProcessor
 * change. It sets up any transactions associated with requests that change the
 * state of the system. It counts on BebincaServer to update
 * outstandingRequests, so that it can take into account transactions that are
 * in the queue to be applied when generating a transaction.
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class RequestPreliminaryProcessor extends UncaughtExceptionExiter implements RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestPreliminaryProcessor.class);

    private HaimaServer server;
    private RequestProcessor nextRequestProcessor;

    private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();

    public RequestPreliminaryProcessor(HaimaServer server, RequestProcessor nextRequestProcessor) {
        super("ProcessThread(sid:" + server.getServerId() + " cport:" + server.getClientPortOfRequestProcessor() + "):", server.getServerListener());
        this.server = server;
        LOG.info(LogUtils.getMessage("Server is set"));
        this.nextRequestProcessor = nextRequestProcessor;
        LOG.info(LogUtils.getMessage("Next request processor thread is set"));
    }

    @Override
    public void run() {
        LOG.info(LogUtils.getMessage("Request preliminary processor thread starts running"));
        try {
            while (true) {
                Request request = requestQueue.take();
                // todo v.y. trace
//                long traceMask = BETrace.CLIENT_REQUEST_TRACE_MASK;
//                if (request.type == OpCode.ping) {
//                    traceMask = BETrace.CLIENT_PING_TRACE_MASK;
//                }
//                if (LOG.isTraceEnabled()) {
//                    BETrace.logRequest(LOG, traceMask, 'P', request, "");
//                }
                if (REQUEST_OF_DEATH == request) {
                    break;
                }
                internalProcessRequest(request);
            }
        } catch (Exception e) {
            handleException(this.getName(), e);
        }
        LOG.info(LogUtils.getMessage("Request preliminary processor thread completes running"));
    }

    @Override
    public void processRequest(Request request) {
        requestQueue.add(request);
    }

    @Override
    public void shutdown() {
        requestQueue.clear();
        requestQueue.add(REQUEST_OF_DEATH);
        nextRequestProcessor.shutdown();
        LOG.info(LogUtils.getMessage("Request preliminary processor is shut down"));
    }

    /**
     * This method will be called inside the ProcessRequestThread, which is a
     * singleton, so there will be a single thread calling this code.
     *
     * @param request
     */
    private void internalProcessRequest(Request request) throws Exception {

        // LOG.info("Prep>>> cxid = " + request.cxid + " type = " +
        // request.type + " id = 0x" + Long.toHexString(request.sessionId));
        request.setTransactionHeader(null);
        request.setTransaction(null);

        try {
            switch (request.getType()) {
                // todo v.y. comment for now
                case OpCode.create:
                    CreateRequest createRequest = new CreateRequest();
                    pRequest2Txn(request.getType(), request, createRequest, true);
                    break;
                case OpCode.delete:
                    DeleteRequest deleteRequest = new DeleteRequest();
                    pRequest2Txn(request.getType(), request, deleteRequest, true);
                    break;
                case OpCode.setData:
                    SetDataRequest setDataRequest = new SetDataRequest();
                    pRequest2Txn(request.getType(), request, setDataRequest, true);
                    break;
                case OpCode.check:
                    CheckVersionRequest checkRequest = new CheckVersionRequest();
                    pRequest2Txn(request.getType(), request, checkRequest, true);
                    break;
                case OpCode.multi:
                    MultiTransactionRecord multiRequest = new MultiTransactionRecord();
                    try {
                        ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), multiRequest);
                    } catch(IOException e) {
                        request.setTransactionHeader(new TxnHeader(request.getSessionId(), getSystemCurrentTimeMillis(), OpCode.multi));
                        throw e;
                    }
                    List<Txn> txns = new ArrayList<Txn>();
                    // Each op in a multi-op must have the same zxid!
                    HaimaException hmEx = null;

                    // Store off current pending change records in case we need to rollback
                    HashMap<String, ChangeRecord> pendingChanges = getPendingChangeRecords(multiRequest);

                    int index = 0;
                    for(Op op: multiRequest) {
                        Record subrequest = op.toRequestRecord() ;

                        /*
                         * If we've already failed one of the ops, don't bother
                         * trying the rest as we know it's going to fail and it
                         * would be confusing in the logfiles.
                         */
                        if (hmEx != null) {
                            request.getTransactionHeader().setType(OpCode.error);
                            request.setTransaction(new ErrorTxn(HaimaException.Code.RUNTIMEINCONSISTENCY.intValue()));
                        }

                        /* Prep the request and convert to a Txn */
                        else {
                            try {
                                pRequest2Txn(op.getType(), request, subrequest, false);
                            } catch (HaimaException e) {
                                if (hmEx == null) {
                                    hmEx = e;
                                }
                                request.getTransactionHeader().setType(OpCode.error);
                                request.setTransaction(new ErrorTxn(e.code().intValue()));
                                LOG.info("Got user-level KeeperException when processing "
                                        + request.toString() + " aborting remaining multi ops."
                                        + " Error Path:" + e.getPath()
                                        + " Error:" + e.getMessage());

                                request.setException(e);

                            }
                        }

                        //FIXME: I don't want to have to serialize it here and then
                        //       immediately deserialize in next processor. But I'm
                        //       not sure how else to get the txn stored into our list.
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
                        request.getTransaction().serialize(boa, "request") ;
                        ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());

                        txns.add(new Txn(request.getTransactionHeader().getType(), bb.array()));
                        index++;
                    }

                    request.setTransactionHeader(new TxnHeader(request.getSessionId(), getSystemCurrentTimeMillis(), request.getType()));
                    request.setTransaction(new MultiTxn(txns));

                    break;

                // todo v.y. depend on client
                //create/close session don't require request record
                case OpCode.createSession:
                case OpCode.closeSession:
                    pRequest2Txn(request.getType(), request, null, true);
                    break;

                //All the rest don't need to create a Txn - just verify session
                // todo v.y. depend on client
                case OpCode.snowFlake:

                // todo v.y. comment for now
                case OpCode.sync:
                case OpCode.exists:
                case OpCode.getData:
                case OpCode.getChildren:
                case OpCode.getChildren2:
                case OpCode.ping:

                // todo v.y. depend on client
                server.getSessionTracker().checkSession(request.getSessionId(), null);
                break;
                default:
                    break;
            }
        } catch (HaimaException e) {
            if (request.getTransactionHeader() != null) {
                request.getTransactionHeader().setType(OpCode.error);
                request.setTransaction(new ErrorTxn(e.code().intValue()));
            }
            LOG.info("Got user-level KeeperException when processing "
                    + request.toString()
                    + " Error Path:" + e.getPath()
                    + " Error:" + e.getMessage());
            request.setException(e);
        } catch (Exception e) {
            // log at error level as we are returning a marshalling
            // error to the user
            LOG.error("Failed to process " + request, e);

            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.getIncomingBuffer();
            if(bb != null){
                bb.rewind();
                while (bb.hasRemaining()) {
                    sb.append(Integer.toHexString(bb.get() & 0xff));
                }
            } else {
                sb.append("request buffer is null");
            }

            LOG.error("Dumping request buffer: 0x" + sb.toString());
            if (request.getTransactionHeader() != null) {
                request.getTransactionHeader().setType(OpCode.error);
                request.setTransaction(new ErrorTxn(HaimaException.Code.MARSHALLINGERROR.intValue()));
            }
        }

        nextRequestProcessor.processRequest(request);
    }

    /**
     * This method will be called inside the ProcessRequestThread, which is a
     * singleton, so there will be a single thread calling this code.
     *
     * @param type
     * @param request
     * @param record
     */
    private void pRequest2Txn(int type, Request request, Record record, boolean deserialize) throws Exception {
        request.setTransactionHeader(new TxnHeader(request.getSessionId(), getSystemCurrentTimeMillis(), type));

        switch (type) {
            case OpCode.create:
                server.getSessionTracker().checkSession(request.getSessionId(), null);
                CreateRequest createRequest = (CreateRequest)record;
                if (deserialize) {
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), createRequest);
                }
                String path = createRequest.getPath();
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1) {
                    LOG.info("Invalid path " + path + " with session 0x" + Long.toHexString(request.getSessionId()));
                    throw new HaimaException.BadArgumentsException(path);
                }

                String parentPath = path.substring(0, lastSlash);
                ChangeRecord parentRecord = getRecordForPath(parentPath);

                int parentCVersion = parentRecord.getStat().getCversion();
                CreateMode createMode =
                        CreateMode.fromFlag(createRequest.getFlags());
                if (createMode.isSequential()) {
                    path = path + String.format(Locale.ENGLISH, "%010d", parentCVersion);
                }
                validatePath(path, request.getSessionId());
                try {
                    if (getRecordForPath(path) != null) {
                        throw new HaimaException.NodeExistsException(path);
                    }
                } catch (HaimaException.NoNodeException e) {
                    // ignore this one
                }
                boolean ephemeralParent = parentRecord.getStat().getEphemeralOwner() != 0;
                if (ephemeralParent) {
                    throw new HaimaException.NoChildrenForEphemeralsException(path);
                }
                int newCversion = parentRecord.getStat().getCversion()+1;
                request.setTransaction(new CreateTxn(path, createRequest.getData(), newCversion));
                StatPersisted s = new StatPersisted();

                parentRecord = parentRecord.duplicate();
                parentRecord.incrementChildCount();
                parentRecord.getStat().setCversion(newCversion);
                addChangeRecord(parentRecord);
                addChangeRecord(new ChangeRecord(path, s, 0));
                break;
            case OpCode.delete:
                server.getSessionTracker().checkSession(request.getSessionId(), null);
                DeleteRequest deleteRequest = (DeleteRequest)record;
                if (deserialize) {
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), deleteRequest);
                }
                path = deleteRequest.getPath();
                lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1 || server.getDatabase().isSpecialPath(path)) {
                    throw new HaimaException.BadArgumentsException(path);
                }
                parentPath = path.substring(0, lastSlash);
                parentRecord = getRecordForPath(parentPath);
                ChangeRecord nodeRecord = getRecordForPath(path);
                int version = deleteRequest.getVersion();
                if (version != -1 && nodeRecord.getStat().getVersion() != version) {
                    throw new HaimaException.BadVersionException(path);
                }
                if (nodeRecord.getChildCount() > 0) {
                    throw new HaimaException.NotEmptyException(path);
                }
                request.setTransaction(new DeleteTxn(path));
                parentRecord = parentRecord.duplicate();
                parentRecord.incrementChildCount();
                addChangeRecord(parentRecord);
                addChangeRecord(new ChangeRecord(path, null, -1));
                break;
            case OpCode.setData:
                server.getSessionTracker().checkSession(request.getSessionId(), null);
                SetDataRequest setDataRequest = (SetDataRequest)record;
                if (deserialize) {
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), setDataRequest);
                }
                path = setDataRequest.getPath();
                validatePath(path, request.getSessionId());
                nodeRecord = getRecordForPath(path);
                version = setDataRequest.getVersion();
                int currentVersion = nodeRecord.getStat().getVersion();
                if (version != -1 && version != currentVersion) {
                    throw new HaimaException.BadVersionException(path);
                }
                version = currentVersion + 1;
                request.setTransaction(new SetDataTxn(path, setDataRequest.getData(), version));
                nodeRecord = nodeRecord.duplicate();
                nodeRecord.getStat().setVersion(version);
                addChangeRecord(nodeRecord);
                break;
            case OpCode.createSession:
                request.getIncomingBuffer().rewind();
                int to = request.getIncomingBuffer().getInt();
                request.setTransaction(new CreateSessionTxn(to));
                request.getIncomingBuffer().rewind();
                server.getSessionTracker().addSession(request.getSessionId(), to);
                break;
            case OpCode.closeSession:
                // We don't want to do this check since the session expiration thread
                // queues up this operation without being the session owner.
                // this request is the last of the session so it should be ok
                // server.getSessionTracker().checkSession(request.sessionId, request.getOwner());
                server.getSessionTracker().setSessionClosing(request.getSessionId());

                LOG.info("Processed session termination for sessionid: 0x"
                        + Long.toHexString(request.getSessionId()));
                break;
            case OpCode.check:
                server.getSessionTracker().checkSession(request.getSessionId(), null);
                CheckVersionRequest checkVersionRequest = (CheckVersionRequest)record;
                if (deserialize) {
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), checkVersionRequest);
                }
                path = checkVersionRequest.getPath();
                validatePath(path, request.getSessionId());
                nodeRecord = getRecordForPath(path);
                version = checkVersionRequest.getVersion();
                currentVersion = nodeRecord.getStat().getVersion();
                if (version != -1 && version != currentVersion) {
                    throw new HaimaException.BadVersionException(path);
                }
                version = currentVersion + 1;
                request.setTransaction(new CheckVersionTxn(path, version));
                break;
        }
    }

    private void addChangeRecord(ChangeRecord c) {
        synchronized (server.getOutstandingChanges()) {
            server.getOutstandingChanges().add(c);
            server.getOutstandingChangesForPath().put(c.getPath(), c);
        }
    }

    private ChangeRecord getRecordForPath(String path) throws HaimaException.NoNodeException {
        ChangeRecord lastChange = null;
        synchronized (server.getOutstandingChanges()) {
            lastChange = server.getOutstandingChangesForPath().get(path);
            if (lastChange == null) {
                DataNode n = server.getDatabase().getNode(path);
                if (n != null) {
                    Set<String> children;
                    synchronized(n) {
                        children = n.getChildren();
                    }
                    lastChange = new ChangeRecord(path, n.getStat(), children.size());
                }
            }
        }
        if (lastChange == null || lastChange.getStat() == null) {
            throw new HaimaException.NoNodeException(path);
        }
        return lastChange;
    }

    /**
     * Grab current pending change records for each op in a multi-op.
     *
     * This is used inside MultiOp error code path to rollback in the event
     * of a failed multi-op.
     *
     * @param multiRequest
     * @return a map that contains previously existed records that probably need to be
     *         rolled back in any failure.
     */
    private HashMap<String, ChangeRecord> getPendingChangeRecords(MultiTransactionRecord multiRequest) {
        HashMap<String, ChangeRecord> pendingChangeRecords = new HashMap<String, ChangeRecord>();

        for (Op op : multiRequest) {
            String path = op.getPath();
            ChangeRecord cr = getOutstandingChangeRecord(path);
            // only previously existing records need to be rolled back.
            if (cr != null) {
                pendingChangeRecords.put(path, cr);
            }

            /*
             * ZOOKEEPER-1624 - We need to store for parent's ChangeRecord
             * of the parent node of a request. So that if this is a
             * sequential node creation request, rollbackPendingChanges()
             * can restore previous parent's ChangeRecord correctly.
             *
             * Otherwise, sequential node name generation will be incorrect
             * for a subsequent request.
             */
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash == -1 || path.indexOf('\0') != -1) {
                continue;
            }
            String parentPath = path.substring(0, lastSlash);
            ChangeRecord parentCr = getOutstandingChangeRecord(parentPath);
            if (parentCr != null) {
                pendingChangeRecords.put(parentPath, parentCr);
            }
        }

        return pendingChangeRecords;
    }
    
    // todo v.y. stats
    private ChangeRecord getOutstandingChangeRecord(String path) {
        synchronized (server.getOutstandingChanges()) {
            return server.getOutstandingChangesForPath().get(path);
        }
    }

    private void validatePath(String path, long sessionId) throws HaimaException.BadArgumentsException {
        try {
            PathUtils.validatePath(path);
        } catch(IllegalArgumentException ie) {
            LOG.info("Invalid path " +  path + " with session 0x" + Long.toHexString(sessionId) +
                    ", reason: " + ie.getMessage());
            throw new HaimaException.BadArgumentsException(path);
        }
    }

    private long getSystemCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
