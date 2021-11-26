package com.demo.haima.client.host;

import java.net.InetSocketAddress;

/**
 * A set of hosts a Haima client should connect to.
 *
 * Classes implementing this interface must guarantee the following:
 *
 * * Every call to next() returns an InetSocketAddress. So the iterator never
 * ends.
 *
 * * The size() of a HostProvider may never be zero.
 *
 * A HostProvider must return resolved InetSocketAddress instances on next(),
 * but it's up to the HostProvider, when it wants to do the resolving.
 *
 * Different HostProvider could be imagined:
 *
 * * A HostProvider that loads the list of Hosts from an URL or from DNS
 * * A HostProvider that re-resolves the InetSocketAddress after a timeout.
 * * A HostProvider that prefers nearby hosts.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public interface HostProvider {

    /**
     * This method is used to get the number of server addresses
     *
     * @return
     */
    int getNumberOfServerAddress();

    /**
     * This method is used to get the next host to try to connect to.
     * For a spinDelay of 0 there should be no wait.
     *
     * @param spinDelay
     *            Milliseconds to wait if all hosts have been tried once.
     */
    InetSocketAddress getNextServerAddress(long spinDelay);

    /**
     * This method is used to notify the HostProvider of a successful connection.
     * The HostProvider may use this notification to reset it's inner state.
     */
    void onConnected();
}
