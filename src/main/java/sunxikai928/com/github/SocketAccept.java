package sunxikai928.com.github;

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
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    socketChannel.configureBlocking(false);
                    Socket socket = new Socket(socketChannel);
                    socketChannel.register(socketRead.readSelector, SelectionKey.OP_READ, socket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
