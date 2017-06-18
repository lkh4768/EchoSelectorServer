import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by WES on 2017-06-10.
 */
public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);
    private static final int FAILED_COUNT_LIMIT = 3;

    private ServerSocketChannel serverSocketChannel = null;
    private Selector selector;
    private Set<Session> sessions = new HashSet<Session>();

    public Server() throws IOException {
        selector = Selector.open();
        initServerSocketChannel();
    }

    private void initServerSocketChannel() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
    }

    public boolean run() {
        try {
            serverSocketChannel.bind(new InetSocketAddress(Config.INSTANCE.getServerPort()));
        } catch (IOException e) {
            logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), " +
                    "HashCode(" + serverSocketChannel.socket().hashCode() + "), Bind Error(" + e.getMessage() + ")");
            return false;
        }

        try {
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), " +
                    "HashCode(" + serverSocketChannel.socket().hashCode() + "), Register Error(" + e.getMessage() +
                    ")");
            return false;
        }

        int failedCnt = 0;
        while (true) {
            if (failedCnt >= FAILED_COUNT_LIMIT) {
                logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), " +
                        "HashCode(" + serverSocketChannel.socket().hashCode() +
                        "), run failed count exceed limit(" + FAILED_COUNT_LIMIT + ")");
                break;
            }

            int readyChannels = 0;
            try {
                readyChannels = selector.select();
            } catch (IOException e) {
                logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), " +
                        "HashCode(" + serverSocketChannel.socket().hashCode() + "), Selector select Error(" +
                        e.getMessage() + ")");
                try {
                    Thread.sleep(1000000 * 10);
                } catch (InterruptedException e1) {
                    logger.error(
                            "Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), " +
                                    "HashCode(" + serverSocketChannel.socket().hashCode() +
                                    "), Selector select Error(" + e.getMessage() + ")");
                }
                failedCnt++;
            }
            failedCnt = 0;

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
                    if (length == 0)
                        continue;

                    session.write(buf, length);
                } else {
                    logger.warn("Invalid Selectkey(" + key.readyOps() + ")");
                }

                keyIterator.remove();
            }
        }

        stop();
        return false;
    }

    private void accept() {
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            Session session = new Session(socketChannel, this);
            sessions.add(session);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(session);
            logger.info("Session(" + session.getHost() + ":" + session.getPort() + "), HashCode(" + session.hashCode() +
                    ") " +
                    "Accepted");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void stop() {
        for (Session session : sessions) {
            session.close();
        }

        sessions.clear();

        if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() +
                        "), HashCode(" + serverSocketChannel.hashCode() + "), Channel Close Error(" + e.getMessage() +
                        ")");
            }
        }

        if (selector != null && selector.isOpen()) {
            try {
                selector.close();
            } catch (IOException e) {
                logger.error("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() +
                        "), Selector HashCode(" + selector.hashCode() + "), Selector Close Error(" + e.getMessage() +
                        ")");
            }
        }

        logger.info("Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + "), Closed");
    }

    public void closeSession(Session session) {
        logger.info("Session(" + session.getHost() + ":" + session.getPort() + "), HashCode(" + session.hashCode() +
                "), Close");
        sessions.remove(session);
        session.close();
    }
}
