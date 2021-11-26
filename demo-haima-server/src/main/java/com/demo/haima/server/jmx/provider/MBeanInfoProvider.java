package com.demo.haima.server.jmx.provider;

/**
 * This interface is used to provider necessary information for
 * registering to JMX (e.g., the information to generate a JMX
 * object name)
 *
 * @author Vince Yuan
 * @date 2021/11/18
 */
public interface MBeanInfoProvider {

    /**
     * This method is used to get the name that identifies the MBean
     *
     * @return a string identifying the MBean
     */
    String getName();

    /**
     * This method is used to tell if an MBean needs to be registered to JMX. <br/>
     * If isHidden returns true, the MBean won't be registered with MBean server,
     * and thus won't be available for management tools. Used for grouping MBeans.
     *
     * @return true if the MBean is hidden.
     */
    boolean isHidden();
}
