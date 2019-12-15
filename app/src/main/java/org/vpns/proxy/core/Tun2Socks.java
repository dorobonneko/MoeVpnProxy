
package org.vpns.proxy.core;

import android.util.Log;

public class Tun2Socks {
    static final {
        System.loadLibrary("tun2socks");
    }

    public static void logTun2Socks(String string2, String string3, String string4) {
        }

    public static native int runTun2Socks(int fd, int mtu, String lan, String mask, String server);

    public static native int terminateTun2Socks();
}

