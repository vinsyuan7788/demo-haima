package com.demo.haima.server.database.data.structure.utils;

/**
 * this class manages quotas and has many other utils for quota
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class Quotas {

    /** the haima nodes that acts as the management and status node **/
    public static final String procHaima = "/haima";

    /** the haima quota node that acts as the quota
     * management node for haima */
    public static final String quotaHaima = "/haima/quota";

    /**
     * the limit node that has the limit of
     * a subtree
     */
    public static final String limitNode = "haima_limits";

    /**
     * the stat node that monitors the limit of
     * a subtree.
     */
    public static final String statNode = "haima_stats";

    /**
     * return the quota path associated with this
     * prefix
     *
     * @param path the actual path in haima.
     * @return the limit quota path
     */
    public static String quotaPath(String path) {
        return quotaHaima + path + "/" + limitNode;
    }

    /**
     * return the stat quota path associated with this
     * prefix.
     *
     * @param path the actual path in haima
     * @return the stat quota path
     */
    public static String statPath(String path) {
        return quotaHaima + path + "/" + statNode;
    }
}
