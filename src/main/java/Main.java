import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by WES on 2017-06-10.
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String args[]) {
        logger.info("Start");
        Config.INSTANCE.getConfig();

        try {
            Server server = new Server();
            server.run();
        } catch (IOException e) {
            logger.error("Server Construction Error(" + e.getMessage() + ")");
            return;
        }
    }
}