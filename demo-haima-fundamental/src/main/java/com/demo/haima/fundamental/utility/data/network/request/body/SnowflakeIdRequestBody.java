package com.demo.haima.fundamental.utility.data.network.request.body;

import com.demo.haima.fundamental.utility.data.network.io.wrapper.InputWrapper;
import com.demo.haima.fundamental.utility.data.network.io.wrapper.OutputWrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class SnowflakeIdRequestBody extends RequestBody {

    private int appCode;

    private SnowflakeIdRequestBody() { }

    private SnowflakeIdRequestBody(int appCode) {
        this.appCode = appCode;
    }

    public int getAppCode() {
        return appCode;
    }

    public static SnowflakeIdRequestBody create() {
        return new SnowflakeIdRequestBody();
    }

    public static SnowflakeIdRequestBody create(int appCode) {
        return new SnowflakeIdRequestBody(appCode);
    }

    @Override
    public void serializeTo(OutputWrapper outputWrapper) {
        outputWrapper.writeInt(appCode);
    }

    @Override
    public void deserializeFrom(InputWrapper inputWrapper) {
        appCode = inputWrapper.readInt();
    }
}
