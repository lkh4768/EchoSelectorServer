import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by WES on 2017-02-18.
 */
public enum Config {
    INSTANCE;
    private String serverIP;
    private int serverPort;
    private int timeout;
    private static final Logger logger = LogManager.getLogger(Config.class);

    public boolean getConfig() {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<XMLConfiguration> builder =
                new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                        .configure(params.xml().setFileName("config.xml"));
        try {
            XMLConfiguration config = builder.getConfiguration();
            serverIP = config.getString("server[@ip]", "127.0.0.1");
            serverPort = config.getInt("server[@port]", 10000);
            timeout = config.getInt("server[@timeout]", 10);

            logger.debug(toString());
        } catch (ConfigurationException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getTimeout() {
        return timeout;
    }

    public String toString() {
        return ("Server / IP(" + serverIP + "), Port(" + serverPort + "), Timeout(" + timeout + ")");
    }
}
