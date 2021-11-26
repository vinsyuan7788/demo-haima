package com.demo.haima.test.integration.demo.utils.provider;

import java.nio.channels.SelectionKey;

/**
 * @author Vince Yuan
 * @date 2021/11/23
 */
public interface SelectionKeyInfoProvider {

    /**
     *  This method is used to log the information of selection keys
     *
     * @param selectedKey
     * @param selectedEvents
     * @param registeredKey
     * @param registeredEvents
     */
    void logSelectionKeyInfo(SelectionKey selectedKey, String selectedEvents, SelectionKey registeredKey, String registeredEvents);
}
