package com.demo.haima.server.jmx.bean;

import com.demo.haima.server.Server;
import com.demo.haima.server.connection.ServerConnection;
import com.demo.haima.server.jmx.bean.mx.ConnectionMXBean;
import com.demo.haima.server.jmx.provider.MBeanInfoProvider;
import com.demo.haima.server.jmx.registry.MBeanRegistry;

import javax.management.ObjectName;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author Vince Yuan
 * @date 2021/11/18
 */
public class ConnectionMBean implements ConnectionMXBean, MBeanInfoProvider {

    private final ServerConnection serverConnection;
//    private final Stats stats;
    private final Server server;
    private final long sessionId;
    private final String remoteIP;

    public ConnectionMBean(ServerConnection serverConnection, Server server) {
        this.serverConnection = serverConnection;
        // todo v.y. stats
//        this.stats = connection;
        this.server = server;
        sessionId = serverConnection.getSessionId();
        InetSocketAddress remoteSocketAddress = (InetSocketAddress) serverConnection.getRemoteSocketAddress();
        if (remoteSocketAddress == null) {
            remoteIP = "Unknown";
        } else {
            InetAddress inetAddress = remoteSocketAddress.getAddress();
            if (inetAddress instanceof Inet6Address) {
                remoteIP = ObjectName.quote(inetAddress.getHostAddress());
            } else {
                remoteIP = inetAddress.getHostAddress();
            }
        }
    }

    @Override
    public String getSessionId() {
        return "0x" + Long.toHexString(sessionId);
    }

    @Override
    public String getName() {
        return MBeanRegistry.getInstance().makeFullPath("Connections", remoteIP, getSessionId());
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
