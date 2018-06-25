package sunxikai928.com.github.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sunxikai928.com.github.Server;

import java.io.IOException;

/**
 * Created by sunxikai on 18/6/21.
 */
public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("服务器启动了");
        try {
            new Server().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
