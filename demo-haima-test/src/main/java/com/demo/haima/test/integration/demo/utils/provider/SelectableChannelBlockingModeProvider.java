package com.demo.haima.test.integration.demo.utils.provider;

import java.nio.channels.SelectableChannel;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public interface SelectableChannelBlockingModeProvider {

    /**
     * This method is used to get the blocking-mode of a selectable channel
     *
     * @param selectableChannel
     * @return
     */
    String getSelectableChannelBlockingMode(SelectableChannel selectableChannel);
}
