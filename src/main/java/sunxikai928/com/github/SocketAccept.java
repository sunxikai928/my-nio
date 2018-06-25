package sunxikai928.com.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 监听端口
 * 接收socket
 * 将socket放入队列
 * Created by sunxikai on 18/6/23.
 */
public class SocketAccept implements Runnable {
    private static Logger log = LoggerFactory.getLogger(SocketAccept.class);

    private int port;
    private SocketRead socketRead;

    public SocketAccept(SocketRead socketRead, int port) {
        this.socketRead = socketRead;
        this.port = port;
    }

    public void run() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            for (; ; ) {
                //本身会阻塞不需要休眠
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    socketChannel.configureBlocking(false);
                    Socket socket = new Socket(socketChannel);
                    socketChannel.register(socketRead.readSelector, SelectionKey.OP_READ, socket);
                    log.info("接收到连接请求:" + socket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
