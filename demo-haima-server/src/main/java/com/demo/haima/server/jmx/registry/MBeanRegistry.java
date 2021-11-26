package com.demo.haima.server.jmx.registry;

import com.demo.haima.common.constant.CommonConstant;
import com.demo.haima.server.jmx.provider.MBeanInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vince Yuan
 * @date 2021/11/18
 */
public class MBeanRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(MBeanRegistry.class);

    private static volatile MBeanRegistry INSTANCE = new MBeanRegistry();

    private MBeanServer mBeanServer;

    private Map<MBeanInfoProvider, String> mBeanAndPathMap = new ConcurrentHashMap<>();

    private MBeanRegistry() {
        try {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        } catch (Error e) {
            LOG.error("Exception when getting platform MBean server, hence create a new one", e);
            // Account for running within IKVM and create a new MBeanServer
            // if the PlatformMBeanServer does not exist.
            mBeanServer = MBeanServerFactory.createMBeanServer();
        }
    }

    /**************************************** Getter and Setter ****************************************/

    public static MBeanRegistry getInstance() {
        return INSTANCE;
    }

    /**************************************** JMX Operation ****************************************/

    /**
     * Registers a new MBean with the platform MBean server.
     *
     * @param bean the bean being registered
     * @param parent if not null, the new bean will be registered as a child
     * node of this parent.
     */
    public void register(MBeanInfoProvider bean, MBeanInfoProvider parent) throws Exception {
        assert bean != null;
        String path = null;
        if (parent != null) {
            path = mBeanAndPathMap.get(parent);
            assert path != null;
        }
        path = makeFullPath(path, parent);
        if (bean.isHidden()) {
            return;
        }
        ObjectName objectName = makeObjectName(path, bean);
        try {
            mBeanServer.registerMBean(bean, objectName);
            mBeanAndPathMap.put(bean, path);
        } catch (JMException e) {
            LOG.warn("Failed to register MBean " + bean.getName());
            throw e;
        }
    }

    /**
     * Unregister MBean.
     *
     * @param bean
     */
    public void unRegister(MBeanInfoProvider bean) {
        if (bean == null) {
            return;
        }
        String path = mBeanAndPathMap.get(bean);
        try {
            unRegister(path, bean);
        } catch (Exception e) {
            LOG.error("Error during unRegister", e);
        }
        mBeanAndPathMap.remove(bean);
    }

    /**************************************** Utility Method ****************************************/

    /**
     * Generate a filesystem-like path.
     *
     * @param prefix
     * @param bean
     * @return
     */
    private String makeFullPath(String prefix, MBeanInfoProvider bean) {
        return makeFullPath(prefix, bean == null ? null : bean.getName());
    }

    /**
     * Generate a filesystem-like path.
     *
     * @param prefix path prefix
     * @param name path elements
     * @return absolute path
     */
    public String makeFullPath(String prefix, String... name) {
        StringBuilder sb = new StringBuilder(prefix == null ?
                "/" :
                ("/".equals(prefix) ?
                        prefix :
                        prefix + "/"
                )
        );
        boolean first = true;
        for (String s : name) {
            if (s == null) {
                continue;
            }
            if (!first) {
                sb.append("/");
            } else {
                first = false;
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Builds an MBean path and creates an ObjectName instance using the path.
     *
     * @param path MBean path
     * @param bean the MBean instance
     * @return ObjectName to be registered with the platform MBean server
     */
    private ObjectName makeObjectName(String path, MBeanInfoProvider bean) throws Exception {
        if (path == null) {
            return null;
        }
        StringBuilder beanName = new StringBuilder(CommonConstant.DOMAIN + ":");
        int counter = 0;
        counter = tokenize(beanName, path, counter);
        tokenize(beanName, bean.getName(), counter);
        beanName.deleteCharAt(beanName.length() - 1);
        try {
            return new ObjectName(beanName.toString());
        } catch (MalformedObjectNameException e) {
            LOG.warn("Invalid name \"" + beanName.toString() + "\" for class " + bean.getClass().toString());
            throw e;
        }
    }

    /**
     * This takes a path, such as /a/b/c, and converts it to
     * name0=a,name1=b,name2=c
     */
    private int tokenize(StringBuilder sb, String path, int index) {
        String[] tokens = path.split("/");
        for (String s: tokens) {
            if (s.length()==0) {
                continue;
            }
            sb.append("name").append(index++).append("=").append(s).append(",");
        }
        return index;
    }

    /**
     * Unregister the MBean identified by the path.
     *
     * @param path
     * @param bean
     */
    private void unRegister(String path, MBeanInfoProvider bean) throws Exception {
        if (path == null) {
            return;
        }
        if (!bean.isHidden()) {
            try {
                mBeanServer.unregisterMBean(makeObjectName(path, bean));
            } catch (JMException e) {
                LOG.warn("Failed to unRegister MBean " + bean.getName());
                throw e;
            }
        }
    }
}
