package com.demo.haima.server.jmx.bean;

import com.demo.haima.server.Server;
import com.demo.haima.server.jmx.bean.mx.ServerMXBean;
import com.demo.haima.server.jmx.provider.MBeanInfoProvider;

import java.util.Date;

/**
 * @author Vince Yuan
 * @date 2021/11/18
 */
public class ServerMBean implements ServerMXBean, MBeanInfoProvider {

    private final Server server;
    private final Date startTime;
    private final String name;

    public ServerMBean(Server server) {
        this.server = server;
        startTime = new Date();
        name = "ServerOnPort" + server.getServerPort();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
