package com.demo.haima.common.serdes.jute.utils;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * This class provides utility methods regarding {@link Record}
 *
 * @author Vince Yuan
 * @date 2021/11/18
 */
public class RecordUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RecordUtils.class);

    private RecordUtils() { }

    /**
     * This method is used to provide a string representation for logging
     *
     * @param record
     * @return
     */
    public static String toString(Record record) {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            if (record == null) {
                return StringUtils.NULL_STRING;
            }
            Class<? extends Record> clazz = record.getClass();
            stringBuffer.append(clazz.getSimpleName());
            stringBuffer.append("[");
            Field[] declaredFields = clazz.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field declaredField = declaredFields[i];
                declaredField.setAccessible(true);
                String fieldName = declaredField.getName();
                Object fieldValue = declaredField.get(record);
                stringBuffer.append(fieldName).append("=").append(fieldValue);
                if (i < declaredFields.length - 1) {
                    stringBuffer.append(";");
                }
            }
            stringBuffer.append("]");
        } catch (Exception e) {
            LOG.error("Exception found", e);
        }
        return stringBuffer.toString();
    }
}
