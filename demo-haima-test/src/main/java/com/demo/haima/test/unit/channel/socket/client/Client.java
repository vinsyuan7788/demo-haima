package com.demo.haima.test.unit.channel.socket.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Vince Yuan
 * @date 2021/12/5
 */
public class Client extends Thread {

    private String clientName;
    private SocketChannel socketChannel;
    private Selector selector;
    private AtomicBoolean write = new AtomicBoolean(false);

    public Client(String clientName) throws Exception {
        this.clientName = clientName;
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(8585));
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    @Override
    public void run() {
        while (!write.get()) { }
        while (socketChannel.isOpen()) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectedKeySet = selector.selectedKeys();
                Iterator<SelectionKey> selectedKeyIterator = selectedKeySet.iterator();
                while (selectedKeyIterator.hasNext()) {
                    SelectionKey selectedKey = selectedKeyIterator.next();
                    if (selectedKey.isAcceptable()) {
                        // Do nothing here
                    }
                    if (selectedKey.isConnectable()) {
                        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
                        while (!clientSocketChannel.finishConnect()) { }
                        clientSocketChannel.register(selector, SelectionKey.OP_WRITE);
                    }
                    if (selectedKey.isReadable()) {
                        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        int numberOfBytesRead = clientSocketChannel.read(byteBuffer);
                        if (numberOfBytesRead <= 0) { continue; }
                        System.out.println(clientName + " receives data: " + new String(byteBuffer.array()));
                        clientSocketChannel.register(selector, SelectionKey.OP_WRITE);
                    }
                    if (selectedKey.isWritable()) {
                        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
                        ByteBuffer byteBuffer = ByteBuffer.wrap("Hello Socket Channel!".getBytes());
                        int numberOfBytesWritten = clientSocketChannel.write(byteBuffer);
                        if (numberOfBytesWritten <= 0) { continue; }
                        clientSocketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    selectedKeyIterator.remove();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void write() throws Exception {
        write.set(true);
    }

    public void close() throws Exception {
        if (socketChannel.socket().isClosed()) { return; }
        socketChannel.socket().shutdownOutput();
        socketChannel.socket().shutdownInput();
        socketChannel.socket().close();
        socketChannel.close();
        join();
    }
}
