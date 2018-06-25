package sunxikai928.com.github;

import java.io.IOException;

/**
 * Created by sunxikai on 18/6/23.
 */
public class Server {

    private int port = 10000;

    //消息处理对象(应用程序)
    public Server() {
    }

    //消息处理对象(应用程序)
    public Server(int port) {
        this.port = port;
    }

    /**
     * 启动 监听线程,处理线程
     */
    public void start() throws IOException {
        SocketWrite socketWrite = new SocketWrite();
        SocketRead socketRead = new SocketRead(socketWrite);
        SocketAccept socketAccept = new SocketAccept(socketRead, port);

        new Thread(socketAccept).start();
        new Thread(socketRead).start();
        // 这样会导致CPU消耗严重
        new Thread(socketWrite).start();

    }
}