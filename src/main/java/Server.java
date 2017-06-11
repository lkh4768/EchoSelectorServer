import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by WES on 2017-06-10.
 */
public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private ServerSocketChannel serverSocketChannel = null;
    private InetSocketAddress addr = null;
    private Selector selector;
    private Set<Session> sessions = new HashSet<Session>();

    Server() throws IOException {
        selector = Selector.open();
        initServerSocketChannel();
        addr = new InetSocketAddress(Config.INSTANCE.getServerPort());
    }

    private void initServerSocketChannel() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
    }

    public void run() {
        try {
            serverSocketChannel.bind(addr);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                int readyChannels = selector.select();
                if (readyChannels == 0) continue;
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        accept();
                    } else if (key.isReadable()) {
                        Session session = (Session) key.attachment();

                        ByteBuffer buf = ByteBuffer.allocate(1024);
                        int length = session.read(buf);
                        if(length== 0)
                            continue;

                        session.write(buf, length);
                    } else {
                        logger.warn("Invalid Selectkey(" + key.readyOps() + ")");
                    }

                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            stop();
        }
    }

    private void accept() {
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            Session session = new Session(socketChannel, this);
            sessions.add(session);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(session);
            logger.info("Session(" + session.getHost() + ":" + session.getPort() + ") Accepted");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void stop() {
        for (Session session : sessions) {
            closeSession(session);
        }

        if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), Channel Close Error(" + e.getMessage() + ")");
            }
        }

        if (selector != null && selector.isOpen()) {
            try {
                selector.close();
            } catch (IOException e) {
                logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), Selector Close Error(" + e.getMessage() + ")");
            }
        }

        logger.info("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), Stop");
    }

    public void closeSession(Session session)
    {
        sessions.remove(session);
        session.close();
    }
}
