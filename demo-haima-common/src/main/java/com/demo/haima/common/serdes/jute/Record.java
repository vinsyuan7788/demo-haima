package com.demo.haima.common.serdes.jute;

import com.demo.haima.common.serdes.jute.deserializer.OutputArchive;
import com.demo.haima.common.serdes.jute.serializer.InputArchive;

import java.io.IOException;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface Record {

    void serialize(OutputArchive archive, String tag) throws IOException;

    void deserialize(InputArchive archive, String tag) throws IOException;
}
