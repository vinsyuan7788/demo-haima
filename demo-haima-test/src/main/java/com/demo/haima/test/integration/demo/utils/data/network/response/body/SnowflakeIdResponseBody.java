package com.demo.haima.test.integration.demo.utils.data.network.response.body;

import com.demo.haima.test.integration.demo.utils.data.network.io.wrapper.InputWrapper;
import com.demo.haima.test.integration.demo.utils.data.network.io.wrapper.OutputWrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class SnowflakeIdResponseBody extends ResponseBody {

    private Long snowflakeId;

    private SnowflakeIdResponseBody() { }

    private SnowflakeIdResponseBody(Long snowflakeId) {
        this.snowflakeId = snowflakeId;
    }

    public Long getSnowflakeId() {
        return snowflakeId;
    }

    public static SnowflakeIdResponseBody create() {
        return new SnowflakeIdResponseBody();
    }

    public static SnowflakeIdResponseBody create(Long snowflakeId) {
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
