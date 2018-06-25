package com.jenkov.nioserver;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by jjenkov on 24-10-2015.
 */
public class Server {

    // 接收器
    private SocketAccepter socketAccepter = null;
    // 处理器
    private SocketProcessor socketProcessor = null;
    // 服务器端口号
    private int tcpPort = 0;
    /**
     * 消息读取者的工厂类,本例中是 {@link com.jenkov.nioserver.http.HttpMessageReaderFactory}
     */
    private IMessageReaderFactory messageReaderFactory = null;
    /**
     * 消息处理对象 本例中是 一个匿名实现 在 Main中
     */
    private IMessageProcessor messageProcessor = null;

    /**
     *
     * @param tcpPort 端口号
     * @param messageReaderFactory 消息读取者的工厂
     * @param messageProcessor 消息处理对象
     */
    public Server(int tcpPort, IMessageReaderFactory messageReaderFactory, IMessageProcessor messageProcessor) {
        this.tcpPort = tcpPort;
        this.messageReaderFactory = messageReaderFactory;
        this.messageProcessor = messageProcessor;
    }

    public void start() throws IOException {
        /**
         * 存放套接字,每次有一个链接就存放一个套接字
         * 然后处理线程从队列中取,有了链接就取出来进行处理
         * 阻塞式的队列,插入之前需要检查是否能够接受更多的套接字,线程安全
         * add: 内部实际上获取的offer方法，当Queue已经满了时，抛出一个异常。不会阻塞。
         * offer:当Queue已经满了时，返回false。不会阻塞。
         * put:当Queue已经满了时，会进入等待，只要不被中断，就会插入数据到队列中。会阻塞，可以响应中断。
         * remove和add相互对应 poll与offer相互对应。take和put相互对应
         *
         * 本例中向队列添加元素使用的是add方法
         */
        Queue socketQueue = new ArrayBlockingQueue(1024); //move 1024 to ServerConfig
        /**
         * 新建接收器,传入端口号和队列
         */
        this.socketAccepter = new SocketAccepter(tcpPort, socketQueue);
        /*
        读写的缓存
         */
        MessageBuffer readBuffer = new MessageBuffer();
        MessageBuffer writeBuffer = new MessageBuffer();
        /**
         * 创建处理器接受到的请求的处理
         * 接受队列,读缓存,写缓存,消息读取者的工厂类,消息处理对象
         */
        this.socketProcessor = new SocketProcessor(socketQueue, readBuffer, writeBuffer, this.messageReaderFactory, this.messageProcessor);
        /**
         * 启动2个线程,一个接受请求放入队列,另一个线程监控队列中有值就立即处理
         */
        Thread accepterThread = new Thread(this.socketAccepter);
        Thread processorThread = new Thread(this.socketProcessor);

        accepterThread.start();
        processorThread.start();
    }


}
