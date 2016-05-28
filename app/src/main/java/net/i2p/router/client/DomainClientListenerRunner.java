package net.i2p.router.client;

import java.io.IOException;
import java.net.ServerSocket;

import net.i2p.client.DomainSocketFactory;
import net.i2p.router.RouterContext;

/**
 * Unix domain socket version of ClientListenerRunner.
 *
 * @author str4d
 * @since 0.9.14
 */
public class DomainClientListenerRunner extends ClientListenerRunner {
    private final DomainSocketFactory factory;

    public DomainClientListenerRunner(RouterContext context, ClientManager manager) {
        super(context, manager, -1);
        factory = new DomainSocketFactory(_context);
    }

    /**
     * @throws IOException
     */
    @Override
    protected ServerSocket getServerSocket() throws IOException {
        return factory.createServerSocket(DomainSocketFactory.I2CP_SOCKET_ADDRESS);
    }
}
