package net.i2p.router.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import net.i2p.client.DomainSocketFactory;
import net.i2p.router.RouterContext;
import net.i2p.util.Log;

/**
 * Unix domain socket version of ClientListenerRunner.
 *
 * @author str4d
 * @since 0.9.14
 */
public class DomainClientListenerRunner extends ClientListenerRunner {
    private final DomainSocketFactory factory;
    private final Log _log;

    public DomainClientListenerRunner(RouterContext context, ClientManager manager) {
        super(context, manager, -1);
        factory = new DomainSocketFactory(_context);
        _log = context.logManager().getLog(getClass());
    }

    /**
     * @throws IOException
     */
    @Override
    protected ServerSocket getServerSocket() throws IOException {
        return factory.createServerSocket(DomainSocketFactory.I2CP_SOCKET_ADDRESS);
    }

    @Override
    public void stopListening() {
        _running = false;
        // LocalServerSocket.close() fails silently if the socket is blocking in accept(), so we
        // trick the socket by opening a new connection and then immediately closing it.
        // http://stackoverflow.com/questions/8007982/java-serversocket-and-android-localserversocket
        try {
            _log.debug("Connecting to domain socket to trigger close");
            Socket s = factory.createSocket(DomainSocketFactory.I2CP_SOCKET_ADDRESS);
            s.close();
        } catch (IOException e) {
            _log.error("Failed to connect to domain socket to trigger close", e);
        }
        // runServer() will close the LocalServerSocket.
    }
}
