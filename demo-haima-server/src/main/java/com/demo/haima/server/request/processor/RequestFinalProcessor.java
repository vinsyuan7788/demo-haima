package com.demo.haima.server.request.processor;

import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.definition.OpResult;
import com.demo.haima.common.definition.OpResult.CheckResult;
import com.demo.haima.common.definition.OpResult.CreateResult;
import com.demo.haima.common.definition.OpResult.DeleteResult;
import com.demo.haima.common.definition.OpResult.ErrorResult;
import com.demo.haima.common.definition.OpResult.SetDataResult;
import com.demo.haima.common.definition.ReplyCode;
import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.exception.HaimaException.Code;
import com.demo.haima.common.io.ByteBufferInputStream;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.request.ExistsRequest;
import com.demo.haima.common.serdes.jute.request.GetChildren2Request;
import com.demo.haima.common.serdes.jute.request.GetChildrenRequest;
import com.demo.haima.common.serdes.jute.request.GetDataRequest;
import com.demo.haima.common.serdes.jute.request.SnowFlakeRequest;
import com.demo.haima.common.serdes.jute.request.SyncRequest;
import com.demo.haima.common.serdes.jute.response.CreateResponse;
import com.demo.haima.common.serdes.jute.response.ExistsResponse;
import com.demo.haima.common.serdes.jute.response.GetChildren2Response;
import com.demo.haima.common.serdes.jute.response.GetChildrenResponse;
import com.demo.haima.common.serdes.jute.response.GetDataResponse;
import com.demo.haima.common.serdes.jute.response.MultiResponse;
import com.demo.haima.common.serdes.jute.response.SetDataResponse;
import com.demo.haima.common.serdes.jute.response.SnowFlakeResponse;
import com.demo.haima.common.serdes.jute.response.SyncResponse;
import com.demo.haima.common.serdes.jute.response.header.ResponseHeader;
import com.demo.haima.common.serdes.jute.statistics.Stat;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.connection.ServerConnection;
import com.demo.haima.server.connection.factory.ServerConnectionFactory;
import com.demo.haima.server.database.data.structure.DataNode;
import com.demo.haima.server.database.data.structure.DataTree;
import com.demo.haima.server.database.transaction.ErrorTxn;
import com.demo.haima.server.database.transaction.header.TxnHeader;
import com.demo.haima.server.request.Request;
import com.demo.haima.server.request.processor.utils.ChangeRecord;
import com.demo.haima.server.snowflake.SnowFlakeIdWorker;
import com.demo.haima.server.snowflake.factory.SnowFlakeIdWorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * This Request processor actually applies any transaction associated with a
 * request and services any queries. It is always at the end of a
 * RequestProcessor chain (hence the name), so it does not have a nextProcessor
 * member.
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class RequestFinalProcessor implements RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestFinalProcessor.class);

    private HaimaServer server;

    public RequestFinalProcessor(HaimaServer server) {
        this.server = server;
        LOG.info(LogUtils.getMessage("Server is set"));
    }

    @Override
    public void processRequest(Request request) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request:: " + request);
        }
        // request.addRQRec(">final");
        // todo v.y. trace
//        long traceMask = BETrace.CLIENT_REQUEST_TRACE_MASK;
//        if (request.type == OpCode.ping) {
//            traceMask = BETrace.SERVER_PING_TRACE_MASK;
//        }
//        if (LOG.isTraceEnabled()) {
//            BETrace.logRequest(LOG, traceMask, 'E', request, "");
//        }
        DataTree.ProcessTxnResult rc = null;
        synchronized (server.getOutstandingChanges()) {
            while (!server.getOutstandingChanges().isEmpty()) {
                ChangeRecord cr = server.getOutstandingChanges().remove(0);
                if (server.getOutstandingChangesForPath().get(cr.getPath()) == cr) {
                    server.getOutstandingChangesForPath().remove(cr.getPath());
                }
            }
            if (request.getTransactionHeader() != null) {
                TxnHeader hdr = request.getTransactionHeader();
                Record txn = request.getTransaction();

                rc = server.processTransaction(hdr, txn);
            }

        }

        if (request.getTransactionHeader() != null && request.getTransactionHeader().getType() == OpCode.closeSession) {
            ServerConnectionFactory serverConnectionFactory = server.getServerConnectionFactory();
            // this might be possible since
            // we might just be playing diffs from the leader
            if (serverConnectionFactory != null && request.getServerConnection() == null) {
                // calling this if we have the connection results in the client's
                // close session response being lost - we've already closed
                // the session/socket here before we can send the closeSession
                // in the switch block below
                serverConnectionFactory.closeSession(request.getSessionId());
                return;
            }
        }

        if (request.getServerConnection() == null) {
            return;
        }
        ServerConnection serverConnection = request.getServerConnection();

        String lastOp = "NA";
        server.decreaseNumberOfRequestsInProcess();
        Code err = Code.OK;
        Record rsp = null;
        boolean closeSession = false;
        try {
            if (request.getTransactionHeader() != null && request.getTransactionHeader().getType() == OpCode.error) {
                throw HaimaException.create(Code.get(((ErrorTxn) request.getTransaction()).getErr()));
            }

            Exception ex = request.getException();
            if (ex != null && request.getType() != OpCode.multi) {
                throw ex;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("{}",request);
            }
            switch (request.getType()) {
                case OpCode.ping: {
                    // todo v.y. stats
//                    server.serverStats().updateLatency(request.createTime);

                    lastOp = "PING";
                    // todo v.y. stats
//                    serverConnection.updateStatsForResponse(lastOp, request.createTime, System.currentTimeMillis());

                    serverConnection.sendResponse(new ResponseHeader(ReplyCode.pingReply, 0), null, "response");
                    return;
                }
                case OpCode.createSession: {
                    // todo v.y. stats
//                    server.serverStats().updateLatency(request.createTime);

                    lastOp = "SESS";
                    // todo v.y. stats
//                    serverConnection.updateStatsForResponse(lastOp, request.createTime, System.currentTimeMillis());

                    server.finishSessionInit(request.getServerConnection(), true);
                    return;
                }
                case OpCode.multi: {
                    lastOp = "MULT";
                    rsp = new MultiResponse() ;

                    for (DataTree.ProcessTxnResult subTxnResult : rc.multiResult) {

                        OpResult subResult ;

                        switch (subTxnResult.type) {
                            case OpCode.check:
                                subResult = new CheckResult();
                                break;
                            case OpCode.create:
                                subResult = new CreateResult(subTxnResult.path);
                                break;
                            case OpCode.delete:
                                subResult = new DeleteResult();
                                break;
                            case OpCode.setData:
                                subResult = new SetDataResult(subTxnResult.stat);
                                break;
                            case OpCode.error:
                                subResult = new ErrorResult(subTxnResult.err) ;
                                break;
                            default:
                                throw new IOException("Invalid type of op");
                        }

                        ((MultiResponse)rsp).add(subResult);
                    }

                    break;
                }
                case OpCode.create: {
                    lastOp = "CREA";
                    rsp = new CreateResponse(rc.path);
                    err = Code.get(rc.err);
                    break;
                }
                case OpCode.delete: {
                    lastOp = "DELE";
                    err = Code.get(rc.err);
                    break;
                }
                case OpCode.setData: {
                    lastOp = "SETD";
                    rsp = new SetDataResponse(rc.stat);
                    err = Code.get(rc.err);
                    break;
                }
                case OpCode.closeSession: {
                    lastOp = "CLOSE";
                    closeSession = true;
                    err = Code.get(rc.err);
                    break;
                }
                case OpCode.sync: {
                    lastOp = "SYNC";
                    SyncRequest syncRequest = new SyncRequest();
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), syncRequest);
                    rsp = new SyncResponse(syncRequest.getPath());
                    break;
                }
                case OpCode.check: {
                    lastOp = "CHEC";
                    rsp = new SetDataResponse(rc.stat);
                    err = Code.get(rc.err);
                    break;
                }
                case OpCode.exists: {
                    lastOp = "EXIS";
                    // TODO we need to figure out the security requirement for this!
                    ExistsRequest existsRequest = new ExistsRequest();
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), existsRequest);
                    String path = existsRequest.getPath();
                    if (path.indexOf('\0') != -1) {
                        throw new HaimaException.BadArgumentsException();
                    }
                    Stat stat = server.getDatabase().statNode(path, existsRequest.getWatch() ? serverConnection : null);
                    rsp = new ExistsResponse(stat);
                    break;
                }
                case OpCode.snowFlake:
                    lastOp = "GESF";
                    SnowFlakeRequest snowFlakeRequest = new SnowFlakeRequest();
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), snowFlakeRequest);
                    int app = snowFlakeRequest.getApp();
                    SnowFlakeIdWorker snowFlakeIdWorker = SnowFlakeIdWorkerFactory.getWorker(app);
                    long snowFlakeId = snowFlakeIdWorker.getNextId();
                    rsp = new SnowFlakeResponse(snowFlakeId);
                    break;
                case OpCode.getData: {
                    lastOp = "GETD";
                    GetDataRequest getDataRequest = new GetDataRequest();
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), getDataRequest);
                    DataNode n = server.getDatabase().getNode(getDataRequest.getPath());
                    if (n == null) {
                        throw new HaimaException.NoNodeException();
                    }
                    Stat stat = new Stat();
                    byte b[] = server.getDatabase().getData(getDataRequest.getPath(), stat);
                    rsp = new GetDataResponse(b, stat);
                    break;
                }
                case OpCode.getChildren: {
                    lastOp = "GETC";
                    GetChildrenRequest getChildrenRequest = new GetChildrenRequest();
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), getChildrenRequest);
                    DataNode n = server.getDatabase().getNode(getChildrenRequest.getPath());
                    if (n == null) {
                        throw new HaimaException.NoNodeException();
                    }

                    List<String> children = server.getDatabase().getChildren(getChildrenRequest.getPath(), null);
                    rsp = new GetChildrenResponse(children);
                    break;
                }
                case OpCode.getChildren2: {
                    lastOp = "GETC";
                    GetChildren2Request getChildren2Request = new GetChildren2Request();
                    ByteBufferInputStream.byteBuffer2Record(request.getIncomingBuffer(), getChildren2Request);
                    Stat stat = new Stat();
                    DataNode n = server.getDatabase().getNode(getChildren2Request.getPath());
                    if (n == null) {
                        throw new HaimaException.NoNodeException();
                    }
                    List<String> children = server.getDatabase().getChildren(getChildren2Request.getPath(), stat);
                    rsp = new GetChildren2Response(children, stat);
                    break;
                }
            }
        } catch (HaimaException.SessionMovedException e) {
            // session moved is a connection level error, we need to tear
            // down the connection otw ZOOKEEPER-710 might happen
            // ie client on slow follower starts to renew session, fails
            // before this completes, then tries the fast follower (leader)
            // and is successful, however the initial renew is then
            // successfully fwd/processed by the leader and as a result
            // the client and leader disagree on where the client is most
            // recently attached (and therefore invalid SESSION MOVED generated)
            serverConnection.sendCloseSession();
            return;
        } catch (HaimaException e) {
            err = e.code();
        } catch (Exception e) {
            // log at error level as we are returning a marshalling
            // error to the user
            LOG.error("Failed to process " + request, e);
            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.getIncomingBuffer();
            bb.rewind();
            while (bb.hasRemaining()) {
                sb.append(Integer.toHexString(bb.get() & 0xff));
            }
            LOG.error("Dumping request buffer: 0x" + sb.toString());
            err = Code.MARSHALLINGERROR;
        }

        ResponseHeader hdr = new ResponseHeader(ReplyCode.requestReply, err.intValue());

        // todo v.y. stats
//        server.serverStats().updateLatency(request.createTime);
//        serverConnection.updateStatsForResponse(lastOp, request.createTime, System.currentTimeMillis());

        try {
            serverConnection.sendResponse(hdr, rsp, "response");
            if (closeSession) {
                serverConnection.sendCloseSession();
            }
        } catch (Exception e) {
            LOG.error("FIXMSG",e);
        }
    }

    @Override
    public void shutdown() {
        // we are the final link in the chain
        LOG.info(LogUtils.getMessage("Request final processor is shut down"));
    }
}
