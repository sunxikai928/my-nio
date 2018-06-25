package com.jenkov.nioserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

/**
 * 链接的接收器
 * Created by jjenkov on 19-10-2015.
 */
public class SocketAccepter implements Runnable {

    // 端口号
    private int tcpPort = 0;
    //服务器套接字通道
    private ServerSocketChannel serverSocket = null;
    //套接字队列
    private Queue socketQueue = null;

    // 接收端口号和套接字队列
    public SocketAccepter(int tcpPort, Queue socketQueue) {
        this.tcpPort = tcpPort;
        this.socketQueue = socketQueue;
    }

    @Override
    public void run() {
        try {
            //创建服务器管道
            this.serverSocket = ServerSocketChannel.open();
            //绑定本地端口号
            this.serverSocket.bind(new InetSocketAddress(tcpPort));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                //监听新进来的连接。当 accept()方法返回的时候,它返回一个包含新进来的连接的 SocketChannel。因此, accept()方法会一直阻塞到有新连接到达
                SocketChannel socketChannel = this.serverSocket.accept();
                // 非阻塞模式,
                // ServerSocketChannel可以设置成非阻塞模式。
                // 在非阻塞模式下，accept() 方法会立刻返回，如果还没有新进来的连接,返回的将是null。
                // 因此，需要检查返回的SocketChannel是否是null.
                // this.serverSocket.configureBlocking(false);
                System.out.println("Socket accepted: " + socketChannel);
                // 将连接包装成自定义的 Socket
                // 将Socket加入队列中
                //todo check if the queue can even accept more sockets.
                // 检查队列是否可以接受更多的套接字。
                this.socketQueue.add(new Socket(socketChannel));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
