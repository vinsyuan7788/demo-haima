package com.demo.haima.test.unit.channel.socket.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/12/5
 */
public class Server extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public Server() throws Exception {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(8585));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        while (serverSocketChannel.isOpen()) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectedKeySet = selector.selectedKeys();
                Iterator<SelectionKey> selectedKeyIterator = selectedKeySet.iterator();
                while (selectedKeyIterator.hasNext()) {
                    SelectionKey selectedKey = selectedKeyIterator.next();
                    if (selectedKey.isAcceptable()) {
                        SocketChannel acceptedSocketChannel = serverSocketChannel.accept();
                        LOG.info("Server accepts connection from client: {}", acceptedSocketChannel.getRemoteAddress());
                        acceptedSocketChannel.configureBlocking(false);
                        acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    if (selectedKey.isConnectable()) {
                        // Do nothing here
                    }
                    if (selectedKey.isReadable()) {
                        SocketChannel acceptedSocketChannel = (SocketChannel) selectedKey.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        int numberOfBytesRead = acceptedSocketChannel.read(byteBuffer);
                        if (numberOfBytesRead <= 0) { continue; }
                        byte[] byteArray = byteBuffer.array();
                        LOG.info("Server reads data: {}", new String(byteArray));
                        acceptedSocketChannel.register(selector, SelectionKey.OP_WRITE, byteArray);
                    }
                    if (selectedKey.isWritable()) {
                        SocketChannel acceptedSocketChannel = (SocketChannel) selectedKey.channel();
                        byte[] byteArray = (byte[]) selectedKey.attachment();
                        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
                        int numberOfBytesWritten = acceptedSocketChannel.write(byteBuffer);
                        if (numberOfBytesWritten <= 0) { continue; }
                        acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    selectedKeyIterator.remove();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void close() throws Exception {
        if (serverSocketChannel.socket().isClosed()) { return; }
        serverSocketChannel.socket().close();
        serverSocketChannel.close();
        join();
    }
}
