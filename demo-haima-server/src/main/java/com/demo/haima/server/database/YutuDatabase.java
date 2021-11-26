package com.demo.haima.server.database;

import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.statistics.Stat;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.connection.ServerConnection;
import com.demo.haima.server.database.data.structure.DataNode;
import com.demo.haima.server.database.data.structure.DataTree;
import com.demo.haima.server.database.data.structure.DataTree.ProcessTxnResult;
import com.demo.haima.server.database.transaction.header.TxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class maintains the in memory database of Haima
 * server states that includes the sessions, datatree.
 * It is booted up after reading the data from the disk.
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class YutuDatabase {

    private static final Logger LOG = LoggerFactory.getLogger(YutuDatabase.class);

    private ConcurrentHashMap<Long, Integer> sessionIdAndSessionTimeoutMap;
    /**
     * A flat that signifies if the database is initialized
     */
    private volatile boolean initialized = false;
    /**
     * make sure on a clear you take care of
     * all these members.
     */
    protected DataTree dataTree;

    public YutuDatabase() {
        dataTree = new DataTree();
        sessionIdAndSessionTimeoutMap = new ConcurrentHashMap<>();
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to load the database from the MySQL onto memory.
     *
     * @return the number of the data load from the MySQL
     */
    public long loadDataBase() {
        int count = 0;
        //TODO DATASnap loader
        initialized = true;
        return count;
    }

    /**
     * This method is used to signify the dataTree within the database is initialized
     *
     * @param initialized
     */
    public void setDataTreeInit(boolean initialized) {
        dataTree.setInitialized(initialized);
    }

    /**.
     * This method is used to clear the database.
     * Note to developers - be careful to see that
     * the clear method does clear out all the
     * data structures in zkdatabase.
     */
    public void clear() {
        /*
         * to be safe we just create a new datatree.
         */
        dataTree = new DataTree();
        sessionIdAndSessionTimeoutMap.clear();

        initialized = false;
        LOG.info(LogUtils.getMessage("Database is cleared"));
    }

    /******************************* Getter and Setter *******************************/

    public ConcurrentHashMap<Long, Integer> getSessionIdAndSessionTimeoutMap() {
        return sessionIdAndSessionTimeoutMap;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public DataTree getDataTree() {
        return dataTree;
    }

    /******************************* Transaction Processing *******************************/

    /**
     * the process transaction on the data
     *
     * @param transactionHeader the txnheader for the txn
     * @param transaction the transaction that needs to be processed
     * @return the result of processing the transaction on this datatree/zkdatabase
     */
    public ProcessTxnResult processTransaction(TxnHeader transactionHeader, Record transaction) {
        return dataTree.processTxn(transactionHeader, transaction);
    }

    /******************************* Data Processing *******************************/

    /**
     * return the sessions in the datatree
     *
     * @return the data tree sessions
     */
    public Collection<Long> getSessions() {
        return dataTree.getSessions();
    }

    /**
     * get the datanode for this path
     *
     * @param path the path to lookup
     * @return the datanode for getting the path
     */
    public DataNode getNode(String path) {
        return dataTree.getNode(path);
    }

    /**
     * stat the path
     *
     * @param path the path for which stat is to be done
     * @param serverConnection the serverConnection attached to this request
     * @return the stat of this node
     * @throws HaimaException.NoNodeException
     */
    public Stat statNode(String path, ServerConnection serverConnection) throws HaimaException.NoNodeException {
        return dataTree.statNode(path);
    }

    /**
     * get data and stat for a path
     *
     * @param path the path being queried
     * @param stat the stat for this path
     * @return
     * @throws HaimaException.NoNodeException
     */
    public byte[] getData(String path, Stat stat) throws HaimaException.NoNodeException {
        return dataTree.getData(path, stat);
    }

    /**
     * get children list for this path
     *
     * @param path the path of the node
     * @param stat the stat of the node
     * @return the list of children for this path
     * @throws HaimaException.NoNodeException
     */
    public List<String> getChildren(String path, Stat stat) throws HaimaException.NoNodeException {
        return dataTree.getChildren(path, stat);
    }

    /******************************* Utility Method *******************************/

    /**
     * check if the path is special or not
     *
     * @param path the input path
     * @return true if path is special and false if not
     */
    public boolean isSpecialPath(String path) {
        return dataTree.isSpecialPath(path);
    }
}
