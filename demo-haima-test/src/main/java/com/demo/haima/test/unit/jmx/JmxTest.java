package com.demo.haima.test.unit.jmx;

import com.demo.haima.test.unit.jmx.bean.Investor;
import com.demo.haima.test.unit.jmx.bean.Student;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/18
 */
public class JmxTest {

    private MBeanServer mBeanServer;

    @Before
    public void testGetMBeanServer() {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    @Test
    public void testJmx() throws Exception {
        int count = 1;
        while (true) {
            if (count == 1) {
                Student object = new Student(1L, "Vince");
                ObjectName objectName = new ObjectName("Student:id=1,name=Vince");
                mBeanServer.registerMBean(object, objectName);
                System.out.println("Registering to JMX succeeds!");
                count++;
            } else if (count == 2) {
                Investor object = new Investor(2L, "Violet");
                ObjectName objectName = new ObjectName("Investor:id=2,name=Violet");
                mBeanServer.registerMBean(object, objectName);
                System.out.println("Registering to JMX succeeds!");
                count++;
            }
        }
    }
}
