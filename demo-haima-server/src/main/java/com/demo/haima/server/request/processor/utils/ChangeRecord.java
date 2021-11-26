package com.demo.haima.server.request.processor.utils;

import com.demo.haima.server.database.data.statistics.StatPersisted;

/**
 * This structure is used to facilitate information sharing between
 * RequestPreliminaryProcess and RequestFinalProcessor
 *
 * @author Vince Yuan
 * @date 2021/11/13
 */
public class ChangeRecord {

    private String path;
    /**
     * Make sure to create a new object when changing
     */
    private StatPersisted stat;
    int childCount;

    public ChangeRecord(String path, StatPersisted stat, int childCount) {
        this.path = path;
        this.stat = stat;
        this.childCount = childCount;
    }

    public String getPath() {
        return path;
    }
    public StatPersisted getStat() {
        return stat;
    }
    public int getChildCount() {
        return childCount;
    }
    public void incrementChildCount() {
        childCount++;
    }

    public ChangeRecord duplicate() {
        StatPersisted stat = new StatPersisted();
        if (this.stat != null) {
            StatPersisted.copy(this.stat, stat);
        }
        return new ChangeRecord(path, stat, childCount);
    }
}
