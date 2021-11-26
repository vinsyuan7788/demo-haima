package com.demo.haima.test.integration.demo.utils.data.network.request.body;

import com.demo.haima.test.integration.demo.utils.data.network.definition.DataType;
import com.demo.haima.test.integration.demo.utils.data.network.io.wrapper.InputWrapper;
import com.demo.haima.test.integration.demo.utils.data.network.io.wrapper.OutputWrapper;
import com.demo.haima.test.integration.demo.utils.exception.DemoException;

/**
 * @author Vince Yuan
 * @date 2021/11/22
 */
public class TransmitDataRequestBody extends RequestBody {

    private int dataType;
    private Object data;

    private TransmitDataRequestBody() { }

    private TransmitDataRequestBody(int dataType, Object data) {
        this.dataType = dataType;
        this.data = data;
    }

    public int getDataType() {
        return dataType;
    }

    public Object getData() {
        return data;
    }

    public static TransmitDataRequestBody create() {
        return new TransmitDataRequestBody();
    }

    public static <T> TransmitDataRequestBody create(T data) {
        if (data instanceof Boolean) {
            return new TransmitDataRequestBody(DataType.BOOLEAN, data);
        } else if (data instanceof Integer) {
            return new TransmitDataRequestBody(DataType.INTEGER, data);
        } else if (data instanceof Long) {
            return new TransmitDataRequestBody(DataType.LONG, data);
        } else if (data instanceof String) {
            return new TransmitDataRequestBody(DataType.STRING, data);
        } else {
            throw new DemoException("Data type is unknown: " + data);
        }
    }

    @Override
    public void serializeTo(OutputWrapper outputWrapper) {
        outputWrapper.writeInt(dataType);
        switch (dataType) {
            case DataType.BOOLEAN:
                outputWrapper.writeBool((boolean) data);
                break;
            case DataType.INTEGER:
                outputWrapper.writeInt((int) data);
                break;
            case DataType.LONG:
                outputWrapper.writeLong((long) data);
                break;
            case DataType.STRING:
                outputWrapper.writeString((String) data);
                break;
            default:
                throw new DemoException("Data type is unknown: " + dataType);
        }
    }

    @Override
    public void deserializeFrom(InputWrapper inputWrapper) {
        dataType = inputWrapper.readInt();
        switch (dataType) {
            case DataType.INTEGER:
                data = inputWrapper.readInt();
                break;
            case DataType.LONG:
                data = inputWrapper.readLong();
                break;
            case DataType.STRING:
                data = inputWrapper.readString();
                break;
            default:
                throw new DemoException("Data type is unknown: " + dataType);
        }
    }
}
