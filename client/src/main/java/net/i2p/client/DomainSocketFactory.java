package net.i2p.client;

import android.net.LocalSocket;

import net.i2p.I2PAppContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Bridge to Android implementation of Unix domain sockets.
 *
 * @author str4d
 * @since 0.9.14
 */
public class DomainSocketFactory {
    public static String I2CP_SOCKET_ADDRESS = "net.i2p.android.client.i2cp";

    public DomainSocketFactory(I2PAppContext context) {
    }

    public Socket createSocket(String name) throws IOException {
        return new DomainSocket(name);
    }

    public Socket createSocket(LocalSocket localSocket) {
        return new DomainSocket(localSocket);
    }

    public ServerSocket createServerSocket(String name) throws IOException {
        return new DomainServerSocket(name, this);
    }
}
