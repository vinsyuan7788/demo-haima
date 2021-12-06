package com.demo.haima.test.unit.channel.socket.client;

import com.demo.haima.test.unit.channel.socket.client.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Vince Yuan
 * @date 2021/12/5
 */
public class Client extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    private Thread clientThread;
    private String clientName;
    private SocketChannel socketChannel;
    private Selector selector;
    private final ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue();
    private final ConcurrentHashMap<String, Packet> idAndPacketMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CountDownLatch> idAndLatchMap = new ConcurrentHashMap<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final String separator_append = " | ";
    private final String separator_split = " \\| ";

    public Client(String clientName) throws Exception {
        clientThread = new Thread(this, clientName);
        this.clientName = clientName;
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(8585));
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    @Override
    public void run() {
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
                        connected.set(true);
                        clientSocketChannel.register(selector, SelectionKey.OP_WRITE);
                    }
                    if (selectedKey.isReadable()) {
                        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        int numberOfBytesRead = clientSocketChannel.read(byteBuffer);
                        if (numberOfBytesRead <= 0) { continue; }
                        String idAndData = new String(byteBuffer.array()).trim();
                        LOG.info("{} receives data: {}", clientName, idAndData);
                        String id = idAndData.split(separator_split)[0];
                        String data = idAndData.split(separator_split)[1];
                        Packet packet = idAndPacketMap.remove(id);
                        packet.setDataReceived(data);
                        synchronized (packet) {
                            packet.notifyAll();
                        }
                        clientSocketChannel.register(selector, SelectionKey.OP_WRITE);
                    }
                    if (selectedKey.isWritable()) {
                        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
                        Packet packet = packetQueue.poll();
                        if (packet == null) { continue; }
                        String idAndData = packet.getId() + separator_append + packet.getDataToSend();
                        LOG.info("{} writes data: {}", clientName, idAndData);
                        ByteBuffer byteBuffer = ByteBuffer.wrap(idAndData.getBytes());
                        int numberOfBytesWritten = clientSocketChannel.write(byteBuffer);
                        if (numberOfBytesWritten <= 0) { continue; }
                        idAndPacketMap.put(packet.getId(), packet);
                        clientSocketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    selectedKeyIterator.remove();
                }
            } catch (Throwable t) {
                if (t instanceof CancelledKeyException) {
                    // Do nothing here
                } else {
                    t.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start() {
        clientThread.start();
    }

    public void close() throws Exception {
        if (socketChannel.socket().isClosed()) { return; }
        socketChannel.socket().shutdownOutput();
        socketChannel.socket().shutdownInput();
        socketChannel.socket().close();
        socketChannel.close();
        join();
    }

    public boolean isConnected() {
        return connected.get();
    }

    public String write(String data) throws Exception {
        Packet packet = new Packet(data);
        packetQueue.offer(packet);
        synchronized (packet) {
            packet.wait();
        }
        return packet.getDataReceived();
    }
}
