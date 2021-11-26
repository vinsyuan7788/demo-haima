package com.demo.haima.server.jmx.bean;

import com.demo.haima.server.database.data.structure.DataTree;
import com.demo.haima.server.jmx.bean.mx.DataTreeMXBean;
import com.demo.haima.server.jmx.provider.MBeanInfoProvider;

/**
 * @author Vince Yuan
 * @date 2021/11/18
 */
public class DataTreeMBean implements DataTreeMXBean, MBeanInfoProvider {

    private DataTree dataTree;

    public DataTreeMBean(DataTree dataTree){
        this.dataTree = dataTree;
    }

    @Override
    public String getName() {
        return "DataTree";
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
