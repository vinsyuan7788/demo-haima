package com.demo.haima.fundamental.utils.business.snowflake.factory;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.utils.business.snowflake.SnowFlakeIdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class SnowFlakeIdWorkerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SnowFlakeIdWorkerFactory.class);

    private static final Map<Integer, SnowFlakeIdWorker> snowFlakeIdWorkerIdAndSnowFlakeIdWorkerMap = new HashMap<Integer, SnowFlakeIdWorker>();

    public static void init(int limit, long workerId, long dataCenterId) {
        for (int i = 0; i < limit; i++) {
            snowFlakeIdWorkerIdAndSnowFlakeIdWorkerMap.put(i, new SnowFlakeIdWorker(i, workerId, dataCenterId));
        }
        LOG.info(LogUtils.getMessage("SnowFlake ID worker factory is initialized | the number of SnowFlake ID worker: {}"), snowFlakeIdWorkerIdAndSnowFlakeIdWorkerMap.size());
        for (SnowFlakeIdWorker snowFlakeIdWorker : snowFlakeIdWorkerIdAndSnowFlakeIdWorkerMap.values()) {
            LOG.info(LogUtils.getMessage("{}"), snowFlakeIdWorker);
        }
    }

    public static SnowFlakeIdWorker getWorker(int app) {
        return snowFlakeIdWorkerIdAndSnowFlakeIdWorkerMap.get(app);
    }
}
