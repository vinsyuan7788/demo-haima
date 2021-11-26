package com.demo.haima.common.definition;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface ReplyCode {

    int requestReply = 1;

    int notificationReply = -1;

    int pingReply = -2;

    int snowFlakeReply = -3;

    int exceptionReply = -4;
}