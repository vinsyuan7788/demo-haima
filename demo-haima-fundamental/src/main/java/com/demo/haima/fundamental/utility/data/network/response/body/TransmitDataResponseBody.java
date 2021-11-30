package com.demo.haima.fundamental.utility.data.network.response.body;

import com.demo.haima.fundamental.utility.data.network.io.wrapper.InputWrapper;
import com.demo.haima.fundamental.utility.data.network.io.wrapper.OutputWrapper;

/**
 * @author Vince Yuan
 * @date 2021/11/23
 */
public class TransmitDataResponseBody extends ResponseBody {

    private boolean success;

    private TransmitDataResponseBody() { }

    private TransmitDataResponseBody(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public static TransmitDataResponseBody create() {
        return new TransmitDataResponseBody();
    }

    public static TransmitDataResponseBody create(boolean success) {
        return new TransmitDataResponseBody(success);
    }

    @Override
    public void serializeTo(OutputWrapper outputWrapper) {
        outputWrapper.writeBool(success);
    }

    @Override
    public void deserializeFrom(InputWrapper inputWrapper) {
        success = inputWrapper.readBool();
    }
}
