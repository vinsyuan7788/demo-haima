package com.demo.haima.server.database.data.structure;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;
import com.demo.haima.common.serdes.jute.statistics.Stat;
import com.demo.haima.server.database.data.statistics.StatPersisted;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains the data for a node in the data tree.
 * <p>
 * A data node contains a reference to its parent, a byte array as its data,
 * a stat object, and a set of its children's paths.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class DataNode implements Record {

    /** the parent of this datanode */
    private DataNode parent;

    /** the data for this datanode */
    private byte data[];

    /**
     * the stat for this node that is persisted to disk.
     */
    private StatPersisted stat;

    /**
     * the list of children for this node. note that the list of children string
     * does not contain the parent path -- just the last part of the path. This
     * should be synchronized on except deserializing (for speed up issues).
     */
    private Set<String> children = null;

    private static final Set<String> EMPTY_SET = Collections.emptySet();

    /**
     * default constructor for the datanode
     */
    DataNode() {
        // default constructor
    }

    /**
     * create a DataNode with parent, data, and stat
     *
     * @param parent
     *            the parent of this DataNode
     * @param data
     *            the data to be set
     * @param stat
     *            the stat for this node.
     */
    public DataNode(DataNode parent, byte data[], StatPersisted stat) {
        this.parent = parent;
        this.data = data;
        this.stat = stat;
    }

    public void setParent(DataNode parent) {
        this.parent = parent;
    }
    public DataNode getParent() {
        return parent;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
    public byte[] getData() {
        return data;
    }
    public void setStat(StatPersisted stat) {
        this.stat = stat;
    }
    public StatPersisted getStat() {
        return stat;
    }

    /**
     * Method that inserts a child into the children set
     *
     * @param child
     *            to be inserted
     * @return true if this set did not already contain the specified element
     */
    public synchronized boolean addChild(String child) {
        if (children == null) {
            // let's be conservative on the typical number of children
            children = new HashSet<String>(8);
        }
        return children.add(child);
    }

    /**
     * Method that removes a child from the children set
     *
     * @param child
     * @return true if this set contained the specified element
     */
    public synchronized boolean removeChild(String child) {
        if (children == null) {
            return false;
        }
        return children.remove(child);
    }

    /**
     * convenience method for setting the children for this datanode
     *
     * @param children
     */
    public synchronized void setChildren(HashSet<String> children) {
        this.children = children;
    }

    /**
     * convenience methods to get the children
     *
     * @return the children of this datanode
     */
    public synchronized Set<String> getChildren() {
        if (children == null) {
            return EMPTY_SET;
        }
        return Collections.unmodifiableSet(children);
    }

    public synchronized void copyStat(Stat to) {
        to.setAversion(stat.getAversion());
        to.setCtime(stat.getCtime());
        to.setCzxid(stat.getCzxid());
        to.setMtime(stat.getMtime());
        to.setMzxid(stat.getMzxid());
        to.setPzxid(stat.getPzxid());
        to.setVersion(stat.getVersion());
        to.setEphemeralOwner(stat.getEphemeralOwner());
        to.setDataLength(data == null ? 0 : data.length);
        int numChildren = 0;
        if (this.children != null) {
            numChildren = children.size();
        }
        // when we do the Cversion we need to translate from the count of the creates
        // to the count of the changes (v3 semantics)
        // for every create there is a delete except for the children still present
        to.setCversion(stat.getCversion()*2 - numChildren);
        to.setNumChildren(numChildren);
    }

    @Override
    synchronized public void deserialize(InputArchive archive, String tag) throws IOException {
        archive.startRecord("node");
        data = archive.readBuffer("data");
        stat = new StatPersisted();
        stat.deserialize(archive, "statpersisted");
        archive.endRecord("node");
    }

    @Override
    synchronized public void serialize(OutputArchive archive, String tag) throws IOException {
        archive.startRecord(this, "node");
        archive.writeBuffer(data, "data");
        stat.serialize(archive, "statpersisted");
        archive.endRecord(this, "node");
    }

}
