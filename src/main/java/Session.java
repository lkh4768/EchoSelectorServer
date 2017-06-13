import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by WES on 2017-06-10.
 */
public class Session {
    private static final Logger logger = LogManager.getLogger(Session.class);
    private final SocketChannel socketChannel;
    private final Server server;
    private String host;
    private int port;

    public Session(SocketChannel socketChannel, Server server) {
        this.server = server;
        this.socketChannel = socketChannel;
        try {
            InetSocketAddress addr = (InetSocketAddress) socketChannel.getRemoteAddress();
            this.host = addr.getHostName();
            this.port = addr.getPort();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            server.closeSession(this);
            logger.error("Session Constructor Error(" + e.getMessage() + ")");
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int read(ByteBuffer buf) {
        int nread = 0;
        try {
            nread = socketChannel.read(buf);
            if (nread == -1)
                throw new IOException();

        } catch (IOException e) {
            logger.error("Session(" + host + ":" + port + "), HashCode(" + this.hashCode() + ") Read Error(" +
                    e.getMessage() + ")");
            server.closeSession(this);
            return nread;
        }

        logger.info(
                "Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + ") <- Session(" +
                        host + ":" + port + "), HashCode(" + this.hashCode() + "), Req(" +
                        Util.bufferByteToString(buf, nread) + ":" + nread + ")");
        buf.flip();
        return nread;
    }


    public void write(ByteBuffer buf, int length) {
        int nwrite = 0;
        try {
            nwrite = socketChannel.write(buf);
        } catch (IOException e) {
            logger.error("Session(" + host + ":" + port + "), HashCode(" + this.hashCode() + "), Write Error(" +
                    e.getMessage() + ")");
            server.closeSession(this);
            return;
        }

        if (nwrite != length) {
            logger.error(
                    "Session(" + host + ":" + port + "), HashCode(" + this.hashCode() +
                            ") Write Error(length diff(org(" + length + "):act(" + nwrite + ")))");
            server.closeSession(this);
            return;
        }

        logger.info(
                "Server(" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + ") -> Session(" +
                        host + ":" + port + "), HashCode(" + this.hashCode() + "), Res(" +
                        Util.bufferByteToString(buf, length) + ":" + length + ")");
    }

    public void close() {
        try {
            if (socketChannel != null && socketChannel.isOpen())
                socketChannel.close();
        } catch (IOException e) {
            logger.error("Session(" + host + ":" + port + "), HashCode(" + this.hashCode() + "), Close Error(" +
                    e.getMessage() + ")");
        }
    }
}
