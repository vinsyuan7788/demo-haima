package com.demo.haima.common.definition;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface OpCode {

    int notification = 0;

    int create = 1;

    int delete = 2;

    int exists = 3;

    int getData = 4;

    int setData = 5;

    int getChildren = 8;

    int sync = 9;

    int snowFlake = 10;

    int ping = 11;

    int getChildren2 = 12;

    int check = 13;

    int multi = 14;

    int createSession = -10;

    int closeSession = -11;

    int error = -1;
}
