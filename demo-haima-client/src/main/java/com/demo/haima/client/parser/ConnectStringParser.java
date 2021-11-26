package com.demo.haima.client.parser;

import com.demo.haima.common.utility.PathUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * A parser for Haima Client connect strings.
 * This class is not meant to be seen or used outside of Haima itself.
 * The chrootPath member should be replaced by a Path object in issue
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class ConnectStringParser {

    private static final int DEFAULT_PORT = 7080;

    private final String chrootPath;
    private final ArrayList<InetSocketAddress> serverAddresses = new ArrayList<InetSocketAddress>();

    public ConnectStringParser(String connectString) {
        // parse out chroot, if any
        int off = connectString.indexOf('/');
        if (off >= 0) {
            String chrootPath = connectString.substring(off);
            // ignore "/" chroot spec, same as null
            if (chrootPath.length() == 1) {
                this.chrootPath = null;
            } else {
                PathUtils.validatePath(chrootPath);
                this.chrootPath = chrootPath;
            }
            connectString = connectString.substring(0, off);
        } else {
            this.chrootPath = null;
        }

        String hostsList[] = connectString.split(",");
        for (String host : hostsList) {
            int port = DEFAULT_PORT;
            int pidx = host.lastIndexOf(':');
            if (pidx >= 0) {
                // otherwise : is at the end of the string, ignore
                if (pidx < host.length() - 1) {
                    port = Integer.parseInt(host.substring(pidx + 1));
                }
                host = host.substring(0, pidx);
            }
            serverAddresses.add(InetSocketAddress.createUnresolved(host, port));
        }
    }

    /******************************* Getter and Setter *******************************/

    public String getChrootPath() {
        return chrootPath;
    }

    public ArrayList<InetSocketAddress> getServerAddresses() {
        return serverAddresses;
    }
}
