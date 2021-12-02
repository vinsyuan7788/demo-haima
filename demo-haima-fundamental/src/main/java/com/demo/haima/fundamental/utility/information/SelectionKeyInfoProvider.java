package com.demo.haima.fundamental.utility.information;

import java.nio.channels.SelectionKey;

/**
 * @author Vince Yuan
 * @date 2021/11/23
 */
public interface SelectionKeyInfoProvider {

    /**
     * This method is used to get the selection key event in string
     *
     * @param opCode
     * @return
     */
    String getSelectionKeyEvent(int opCode);

    /**
     *  This method is used to log the information of selection keys
     *
     * @param selectedKey
     * @param selectedOpCode
     * @param registeredKey
     * @param registeredOpCode
     */
    void logSelectionKeyInfo(SelectionKey selectedKey, int selectedOpCode, SelectionKey registeredKey, int registeredOpCode);
}
