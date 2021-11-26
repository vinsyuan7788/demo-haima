package com.demo.haima.client.host;

import com.demo.haima.common.utility.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Most simple HostProvider, resolves only on instantiation.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class StaticHostProvider implements HostProvider {

    private static final Logger LOG = LoggerFactory.getLogger(StaticHostProvider.class);

    private final List<InetSocketAddress> serverAddresses = new ArrayList<InetSocketAddress>(5);

    /**
     * The index of the server address that try to connect to
     */
    private int currentIndex = -1;
    /**
     * The index of the server address that is successfully connected to
     */
    private int lastIndex = -1;

    /**
     * Constructs a SimpleHostSet.
     *
     * @param serverAddresses
     *            possibly unresolved ZooKeeper server addresses
     * @throws UnknownHostException
     *            if there is an exception unknown
     */
    public StaticHostProvider(Collection<InetSocketAddress> serverAddresses) throws UnknownHostException {
        for (InetSocketAddress address : serverAddresses) {
            InetAddress ia = address.getAddress();
            InetAddress resolvedAddresses[] = InetAddress.getAllByName((ia != null) ? ia.getHostAddress(): address.getHostName());
            for (InetAddress resolvedAddress : resolvedAddresses) {
                // If hostName is null but the address is not, we can tell that
                // the hostName is an literal IP address. Then we can set the host string as the hostname
                // safely to avoid reverse DNS lookup.
                // As far as i know, the only way to check if the hostName is null is use toString().
                // Both the two implementations of InetAddress are final class, so we can trust the return value of
                // the toString() method.
                if (resolvedAddress.toString().startsWith("/") && resolvedAddress.getAddress() != null) {
                    this.serverAddresses.add(new InetSocketAddress(InetAddress.getByAddress(address.getHostName(), resolvedAddress.getAddress()), address.getPort()));
                } else {
                    this.serverAddresses.add(new InetSocketAddress(resolvedAddress.getHostAddress(), address.getPort()));
                }
            }
        }
        if (this.serverAddresses.isEmpty()) {
            throw new IllegalArgumentException("A HostProvider may not be empty!");
        }
        LOG.info(LogUtils.getMessage("Server addresses are set | size: {} | content: {}"), serverAddresses.size(), serverAddresses);
        Collections.shuffle(this.serverAddresses);
    }

    @Override
    public int getNumberOfServerAddress() {
        return serverAddresses.size();
    }

    @Override
    public InetSocketAddress getNextServerAddress(long spinDelay) {
        ++currentIndex;
        if (currentIndex == serverAddresses.size()) {
            currentIndex = 0;
        }
        if (currentIndex == lastIndex && spinDelay > 0) {
            try {
                Thread.sleep(spinDelay);
            } catch (InterruptedException e) {
                LOG.warn("Unexpected exception", e);
            }
        } else if (lastIndex == -1) {
            // We don't want to sleep on the first ever connect attempt.
            lastIndex = 0;
        }

        return serverAddresses.get(currentIndex);
    }

    @Override
    public void onConnected() {
        lastIndex = currentIndex;
    }
}
