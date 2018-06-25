package provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.KQueueSelectorProvider;

/**
 * 可通过java的SPI改写Channel的生成规则
 * Created by sunxikai on 18/6/26.
 */
public class MyProvider extends KQueueSelectorProvider {
    private static Logger log = LoggerFactory.getLogger(MyProvider.class);

    public MyProvider() {
        log.info("可自定义channel的生成规则,通过java的SPI");
    }
}
