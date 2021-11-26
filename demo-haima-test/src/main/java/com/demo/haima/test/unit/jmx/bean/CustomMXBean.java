package com.demo.haima.test.unit.jmx.bean;

import javax.management.JMX;
import javax.management.MXBean;

/**
 * This interface is used to a parent class for the sub-classes that need to
 * be registered to JMX to implement. <br/>
 * If annotation {@link MXBean} is NOT used, the name of the interface MUST
 * end with "MXBean" if annotation {@link MXBean}.
 * If annotation {@link MXBean} is used, the name of the interface does not
 * have any restriction.
 * See {@link JMX#isMXBeanInterface(Class)} for more details. <br/>
 * Any bean that is registered to JMX can be inspected by Java command "jconsole",
 * which is a graphical UI console to monitor runtime resources and JMX-managed beans
 *
 * @author Vince Yuan
 * @date 2021/11/18
 */
public interface CustomMXBean {
}
