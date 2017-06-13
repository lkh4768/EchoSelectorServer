import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by WES on 2017-06-10.
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String args[]) {
        if (!Config.INSTANCE.getConfig()) {
            logger.error("Get Config failed");
            return;
        }

        Server server = null;
        try {
            server = new Server();
        } catch (IOException e) {
            logger.error("Server Create Error(" + e.getMessage() + ")");
            return;
        }

        if (server != null)
            server.run();
        else
            logger.error("Server" + Config.INSTANCE.getServerIP() + ":" + Config.INSTANCE.getServerPort() + " is null");
    }
}