package com.demo.haima.server.connection;

import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.response.header.ResponseHeader;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.statistics.provider.ServerConnectionStatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This class is used to represent a connection from the client, only that
 * it is the wrapped version of the connection and it contains more
 * information that the server is concerned about or needed.
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public abstract class ServerConnection implements ServerConnectionStatisticsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServerConnection.class);

    private static final String SNOWFLAKE_4LW_COMMANDS_WHITELIST = "snowflake.4lw.commands.whitelist";

    private final static HashMap<Integer, String> cmd2String = new HashMap<>();

    private static boolean whiteListInitialized = false;
    private static final HashSet<String> whiteListedCommands = new HashSet<>();

    public final static int confCmd = ByteBuffer.wrap("conf".getBytes()).getInt();
    public final static int consCmd = ByteBuffer.wrap("cons".getBytes()).getInt();
    public final static int crstCmd = ByteBuffer.wrap("crst".getBytes()).getInt();
    public final static int dumpCmd = ByteBuffer.wrap("dump".getBytes()).getInt();
    public final static int enviCmd = ByteBuffer.wrap("envi".getBytes()).getInt();
    public final static int getTraceMaskCmd = ByteBuffer.wrap("gtmk".getBytes()).getInt();
    public final static int ruokCmd = ByteBuffer.wrap("ruok".getBytes()).getInt();
    public final static int setTraceMaskCmd = ByteBuffer.wrap("stmk".getBytes()).getInt();
    public final static int srvrCmd = ByteBuffer.wrap("srvr".getBytes()).getInt();
    public final static int srstCmd = ByteBuffer.wrap("srst".getBytes()).getInt();
    public final static int statCmd = ByteBuffer.wrap("stat".getBytes()).getInt();
    public final static int mntrCmd = ByteBuffer.wrap("mntr".getBytes()).getInt();
    public final static int isroCmd = ByteBuffer.wrap("isro".getBytes()).getInt();

    // specify all of the commands that are available
    static {
        cmd2String.put(confCmd, "conf");
        cmd2String.put(consCmd, "cons");
        cmd2String.put(crstCmd, "crst");
        cmd2String.put(dumpCmd, "dump");
        cmd2String.put(enviCmd, "envi");
        cmd2String.put(getTraceMaskCmd, "gtmk");
        cmd2String.put(ruokCmd, "ruok");
        cmd2String.put(setTraceMaskCmd, "stmk");
        cmd2String.put(srstCmd, "srst");
        cmd2String.put(srvrCmd, "srvr");
        cmd2String.put(statCmd, "stat");
        cmd2String.put(mntrCmd, "mntr");
        cmd2String.put(isroCmd, "isro");
        LOG.info(LogUtils.getMessage("ServerConnection#static", "cmd2String: {}"), cmd2String);
    }

//    private final AtomicLong packetsReceived = new AtomicLong();
//    private final AtomicLong packetsSent = new AtomicLong();

    /******************************* Command Processing *******************************/

    /**
     * This method returns the string representation of the specified command code.
     */
    public static String getCommandString(int command) {
        return cmd2String.get(command);
    }

    /**
     * This method checks if the specified command code is from a known command.
     *
     * @param command The integer code of command.
     * @return true if the specified command is known, false otherwise.
     */
    public static boolean isKnown(int command) {
        return cmd2String.containsKey(command);
    }

    /**
     * This method checks if the specified command is enabled.
     *
     * It Introduces a configuration option to only
     * allow a specific set of white listed commands to execute.
     * A command will only be executed if it is also configured
     * in the white list.
     *
     * @param command The command string.
     * @return true if the specified command is enabled.
     */
    public synchronized static boolean isEnabled(String command) {
        if (whiteListInitialized) {
            return whiteListedCommands.contains(command);
        }

        String commandString = System.getProperty(SNOWFLAKE_4LW_COMMANDS_WHITELIST);
        if (commandString != null) {
            String[] commandList = commandString.split(",");
            for (String cmd : commandList) {
                if (cmd.trim().equals("*")) {
                    for (Map.Entry<Integer, String> entry : cmd2String.entrySet()) {
                        whiteListedCommands.add(entry.getValue());
                    }
                    break;
                }
                if (!cmd.trim().isEmpty()) {
                    whiteListedCommands.add(cmd.trim());
                }
            }
        } else {
            for (Map.Entry<Integer, String> entry : cmd2String.entrySet()) {
                String cmd = entry.getValue();
                if (cmd.equals("wchc") || cmd.equals("wchp")) {
                    //  disable these exploitable commands by default.
                    continue;
                }
                whiteListedCommands.add(cmd);
            }
        }

        // Readonly mode depends on "isro".
        if (System.getProperty("readonlymode.enabled", "false").equals("true")) {
            whiteListedCommands.add("isro");
        }
        whiteListedCommands.add("srvr");
        whiteListInitialized = true;
        LOG.info("The list of known four letter word commands is : {}", Arrays.asList(cmd2String));
        LOG.info("The list of enabled four letter word commands is : {}", Arrays.asList(whiteListedCommands));
        return whiteListedCommands.contains(command);
    }

    /******************************* Statistics Processing *******************************/

    // todo v.y. stats
//    public void packetReceived() {
//        incrPacketReceived();
////        ServerStats serverStats = serverStats();
////        if (serverStats != null) {
////            serverStats.incrementPacketsReceived();
////        }
//    }

    // todo v.y. stats
//    public void packetSent() {
//        incrPacketSent();
////        ServerStats serverStats = serverStats();
////        if (serverStats != null) {
////            serverStats.incrementPacketsSent();
////        }
//    }

//    private long incrPacketReceived() {
//        return packetsReceived.incrementAndGet();
//    }
//
//    private long incrPacketSent() {
//        return packetsSent.incrementAndGet();
//    }

    /******************************* Connection Processing *******************************/

    /**
     * This method is used to close the connection and remove it from the factory connections list.
     * It returns immediately if the connection is not on the connections list.
     */
    public abstract void close();

    /**
     * This method is used to disable receiving any packet
     */
    public abstract void disableReceive();

    /**
     * This method is used to enable receiving any packet
     */
    public abstract void enableRecv();

    /**
     * This method is used to send byte buffer
     *
     * @param byteBuffer
     */
    public abstract void sendBuffer(ByteBuffer byteBuffer);

    /**
     * This method is used to send a response.
     * [Process]
     *
     * @param responseHeader
     * @param record
     * @param tag
     * @throws Exception
     */
    public abstract void sendResponse(ResponseHeader responseHeader, Record record, String tag) throws Exception;

    /**
     * This method is used to notify the client the session is closing and close/cleanup socket
     */
    public abstract void sendCloseSession();

    /******************************* Getter and Setter *******************************/

    /**
     * This method is used to get the client address
     *
     * @return
     */
    public abstract SocketAddress getRemoteSocketAddress();

    /**
     * This method is used to set the session (which is passed from request)
     *
     * @param sessionId
     */
    public abstract void setSessionId(long sessionId);

    /**
     * This method is used to get the session ID
     *
     * @return
     */
    public abstract long getSessionId();

    /**
     * This method is used to set the session timeout
     *
     * @param sessionTimeout
     */
    public abstract void setSessionTimeout(int sessionTimeout);

    /**
     * This method is used to get the session timeout
     *
     * @return
     */
    public abstract int getSessionTimeout();
}
