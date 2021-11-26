package com.demo.haima.server.snowflake;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class SnowFlakeIdWorker {

    private final long zlsdepoch = 1510156800000L;

    private final int entityIdBits = 10;
    private final int workerIdBits = 5;
    private final int dataCenterIdBits = 2;
    private final int sequenceIdBits = 7;
    private final long maxEntityId = -1L ^ (-1L << entityIdBits);
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDataCenterId = -1L ^ (-1L << dataCenterIdBits);
    private final long maxSequence = -1L ^ (-1L << sequenceIdBits);

    private final int workerIdShift = sequenceIdBits;
    private final int dataCenterIdShift = sequenceIdBits + workerIdBits;
    private final int entityIdShift = dataCenterIdShift + dataCenterIdBits;
    private final int timestampLeftShift = entityIdBits + entityIdShift;
    private final long sequenceMask = -1L ^ (-1L << sequenceIdBits);

    private long lastTimestamp = 0L;

    private long entityId;
    private long workerId;
    private long dataCenterId;
    private long sequence;

    private final Object lock = new Object();

    public SnowFlakeIdWorker(long entityId, long workerId, long dataCenterId) {
        if (entityId > maxEntityId || entityId < 0) {
            entityId = maxEntityId;
        }
        if (workerId > maxWorkerId || workerId < 0) {
            workerId = maxWorkerId;
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            dataCenterId = maxDataCenterId;
        }
        this.entityId = entityId;
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        this.sequence = 0L;
    }

    public long getNextId() {
        synchronized (lock) {
            long timestamp = System.currentTimeMillis();
            if (timestamp < lastTimestamp) {
                throw new RuntimeException(
                        String.format("Clock moved backwards.  Refusing to generate id for %s milliseconds", lastTimestamp - timestamp));
            }
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & sequenceMask;
                if (sequence == 0) {
                    timestamp = waitNextTimestamp(timestamp);
                }
            } else {
                sequence = timestamp % 2 == 0 ? 0L : 1L;
            }
            lastTimestamp = timestamp;
            long id = ((timestamp - zlsdepoch) << timestampLeftShift) |
                    (entityId << entityIdShift) |
                    (dataCenterId << dataCenterIdShift) |
                    (workerId << workerIdShift) | sequence;
            return id;
        }
    }

    private long waitNextTimestamp(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    @Override
    public String toString() {
        return "SnowFlakeIdWorker{" +
                "entityId=" + entityId +
                ", workerId=" + workerId +
                ", dataCenterId=" + dataCenterId +
                ", zlsdepoch=" + zlsdepoch +
                ", entityIdBits=" + entityIdBits +
                ", workerIdBits=" + workerIdBits +
                ", dataCenterIdBits=" + dataCenterIdBits +
                ", sequenceIdBits=" + sequenceIdBits +
                ", maxEntityId=" + maxEntityId +
                ", maxWorkerId=" + maxWorkerId +
                ", maxDataCenterId=" + maxDataCenterId +
                ", maxSequence=" + maxSequence +
                ", workerIdShift=" + workerIdShift +
                ", dataCenterIdShift=" + dataCenterIdShift +
                ", entityIdShift=" + entityIdShift +
                ", timestampLeftShift=" + timestampLeftShift +
                ", sequenceMask=" + sequenceMask +
                ", lastTimestamp=" + lastTimestamp +
                ", sequence=" + sequence +
                ", lock=" + lock +
                '}';
    }
}
