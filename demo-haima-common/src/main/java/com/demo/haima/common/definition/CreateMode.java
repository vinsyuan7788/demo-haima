package com.demo.haima.common.definition;

import com.demo.haima.common.exception.HaimaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CreateMode value determines how the znode is created on Haima.
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public enum CreateMode {

    /**
     * The znode will not be automatically deleted upon client's disconnect.
     */
    PERSISTENT (0, false, false),
    /**
     * The znode will not be automatically deleted upon client's disconnect,
     * and its name will be appended with a monotonically increasing number.
     */
    PERSISTENT_SEQUENTIAL (2, false, true),
    ;
    private static final Logger LOG = LoggerFactory.getLogger(CreateMode.class);

    private boolean sequential;
    private int flag;

    CreateMode(int flag, boolean ephemeral, boolean sequential) {
        this.flag = flag;
        this.sequential = sequential;
    }

    public boolean isSequential() {
        return sequential;
    }

    public int toFlag() {
        return flag;
    }

    /**
     * Map an integer value to a CreateMode value
     */
    static public CreateMode fromFlag(int flag) throws HaimaException {
        switch(flag) {
            case 0: return CreateMode.PERSISTENT;

            case 2: return CreateMode.PERSISTENT_SEQUENTIAL;


            default:
                String errMsg = "Received an invalid flag value: " + flag
                        + " to convert to a CreateMode";
                LOG.error(errMsg);
                throw new HaimaException.BadArgumentsException(errMsg);
        }
    }
}

