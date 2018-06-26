package sunxikai928.com.github.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sunxikai928.com.github.IMessageProcessor;
import sunxikai928.com.github.Server;

import java.io.IOException;

/**
 * Created by sunxikai on 18/6/21.
 */
public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    private static IMessageProcessor iMessageProcessor = (socket) -> {
        logger.info("request:" + socket.requestMessage);
        // 如果有请求对照的容器就可以实现分发,找到具体的处理逻辑,
        // 就是根据请求路径找具体处理实现,将请求交给具体实现
        // 最后拿到返回值返回
        // 还可以实现同步异步.有 IO / 需要数据库处理 / 需要调用外部接口 的就异步
        // 或者 是否异步通过 注解实现,如果是有异步注解的就异步处理
        // FIXME 异步如何返回待确定
        socket.message = "收到信息:" + socket.requestMessage;
        logger.info("response:" + socket.message);
    };

    public static void main(String[] args) {
        logger.info("服务器启动了");
        try {
            new Server(iMessageProcessor).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
