package sunxikai928.com.github;

import java.io.IOException;

/**
 * Created by sunxikai on 18/6/23.
 */
public class Server {

    private int port = 10000;
    private IMessageProcessor iMessageProcessor;

    //消息处理对象(应用程序)
    public Server(IMessageProcessor iMessageProcessor) {
        this.iMessageProcessor = iMessageProcessor;
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
        SocketRead socketRead = new SocketRead(socketWrite,this.iMessageProcessor);
        SocketAccept socketAccept = new SocketAccept(socketRead, port);

        new Thread(socketAccept,"accept_thread").start();
        new Thread(socketRead,"read_thread").start();
        new Thread(socketWrite,"write_thread").start();

    }
}
