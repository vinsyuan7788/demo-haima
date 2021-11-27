package com.demo.haima.fundamental.utils.data.network.response.body;

import com.demo.haima.fundamental.utils.data.network.io.wrapper.InputWrapper;
import com.demo.haima.fundamental.utils.data.network.io.wrapper.OutputWrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class SnowflakeIdResponseBody extends ResponseBody {

    private long snowflakeId;

    private SnowflakeIdResponseBody() { }

    private SnowflakeIdResponseBody(long snowflakeId) {
        this.snowflakeId = snowflakeId;
    }

    public long getSnowflakeId() {
        return snowflakeId;
    }

    public static SnowflakeIdResponseBody create() {
        return new SnowflakeIdResponseBody();
    }

    public static SnowflakeIdResponseBody create(long snowflakeId) {
        return new SnowflakeIdResponseBody(snowflakeId);
    }

    @Override
    public void serializeTo(OutputWrapper outputWrapper) {
        outputWrapper.writeLong(snowflakeId);
    }

    @Override
    public void deserializeFrom(InputWrapper inputWrapper) {
        snowflakeId = inputWrapper.readLong();
    }
}
