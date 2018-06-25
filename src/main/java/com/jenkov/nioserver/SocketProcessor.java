package com.jenkov.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

/**
 * 套接字处理器
 * Created by jjenkov on 16-10-2015.
 */
public class SocketProcessor implements Runnable {
    // Socket 队列
    private Queue<Socket> inboundSocketQueue = null;
    //todo   Not used now - but perhaps will be later - to check for space in the buffer before reading from sockets
    // 现在不使用，但可能会晚些——在从套接字读取之前检查缓冲区中的空间。
    private MessageBuffer readMessageBuffer = null;
    //todo   Not used now - but perhaps will be later - to check for space in the buffer before reading from sockets (space for more to write?)
    // 现在不使用-但也许稍后会——在从套接字读取空间之前检查缓冲器中的空间（空间用于多写）
    private MessageBuffer writeMessageBuffer = null;
    // 读取的工厂对象
    private IMessageReaderFactory messageReaderFactory = null;
    //todo use a better / faster queue.
    //使用更好的/更快的队列。
    //出站消息队列
    private Queue<Message> outboundMessageQueue = new LinkedList<Message>();
    //socketId 与 socket 的 map
    private Map<Long, Socket> socketMap = new HashMap<Long, Socket>();
    // 读的缓存(所有连接公用)
    private ByteBuffer readByteBuffer = ByteBuffer.allocate(1024 * 1024);
    // 写的缓存(所有连接公用)
    private ByteBuffer writeByteBuffer = ByteBuffer.allocate(1024 * 1024);
    // 读选择器
    private Selector readSelector = null;
    // 写选择器
    private Selector writeSelector = null;
    // 消息处理器
    private IMessageProcessor messageProcessor = null;
    // 写的代理
    private WriteProxy writeProxy = null;
    // socketId
    private long nextSocketId = 16 * 1024; //start incoming socket ids from 16K - reserve bottom ids for pre-defined sockets (servers).
    //空到非空套接字
    private Set<Socket> emptyToNonEmptySockets = new HashSet<Socket>();
    //非空到空套接字
    private Set<Socket> nonEmptyToEmptySockets = new HashSet<Socket>();

    public SocketProcessor(Queue<Socket> inboundSocketQueue, MessageBuffer readMessageBuffer, MessageBuffer writeMessageBuffer, IMessageReaderFactory messageReaderFactory, IMessageProcessor messageProcessor) throws IOException {
        this.inboundSocketQueue = inboundSocketQueue;

        this.readMessageBuffer = readMessageBuffer;
        this.writeMessageBuffer = writeMessageBuffer;
        this.writeProxy = new WriteProxy(writeMessageBuffer, this.outboundMessageQueue);

        this.messageReaderFactory = messageReaderFactory;

        this.messageProcessor = messageProcessor;

        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
    }

    public void run() {
        while (true) {
            try {
                executeCycle();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void executeCycle() throws IOException {
        // 获取新套接字
        takeNewSockets();
        // 从套接字读取数据
        readFromSockets();
        // 向套接字写入数据
        writeToSockets();
    }


    public void takeNewSockets() throws IOException {
        // 从队列中获取最新的连接
        Socket newSocket = this.inboundSocketQueue.poll();
        //如果空说明没有连接进来
        while (newSocket != null) {
            // 为新连接添加id
            newSocket.socketId = this.nextSocketId++;
            // 配置阻塞
            newSocket.socketChannel.configureBlocking(false);
            // 新建消息读取对象本例中是 HttpMessageReader 并赋给socket
            newSocket.messageReader = this.messageReaderFactory.createMessageReader();
            // 在从套接字读取之前检查缓冲区中的空间。
            newSocket.messageReader.init(this.readMessageBuffer);
            //写消息的对象
            newSocket.messageWriter = new MessageWriter();
            // 将新连接存入map id为key
            this.socketMap.put(newSocket.socketId, newSocket);
            // 将新连接注册到读选择器
            SelectionKey key = newSocket.socketChannel.register(this.readSelector, SelectionKey.OP_READ);
            // 将自定义的 Socket 以附件形式保存在key中
            key.attach(newSocket);
            // 取下一个连接
            newSocket = this.inboundSocketQueue.poll();
        }
    }


    public void readFromSockets() throws IOException {
        // selectNow()不会阻塞，不管什么通道就绪都立刻返回
        // （译者注：此方法执行非阻塞的选择操作。如果自从前一次选择操作后，没有通道变成可选择的，则此方法直接返回零。）
        int readReady = this.readSelector.selectNow();
        // 有可读的链接
        if (readReady > 0) {
            // 获得全部可读的链接的key
            Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
            // 遍历
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                //在这里读取数据并对数据做处理,将返回值绑定到
                readFromSocket(key);
                // 将key从集合中删除
                keyIterator.remove();
            }
            //清空集合
            selectedKeys.clear();
        }
    }

    /**
     * 具体的处理请求和响应信息
     *
     * @param key
     * @throws IOException
     */
    private void readFromSocket(SelectionKey key) throws IOException {
        // 获取key中保存的附件(自定义Socket)
        Socket socket = (Socket) key.attachment();
        //读取数据,并解析数据
        socket.messageReader.read(socket, this.readByteBuffer);
        //拿到所有数据片段的集合
        List<Message> fullMessages = socket.messageReader.getMessages();
        if (fullMessages.size() > 0) {
            // todo 每个请求都有一个响应,如果当前没有获取到请求的全部也是一样吗???
            for (Message message : fullMessages) {
                message.socketId = socket.socketId;
                //the message processor will eventually push outgoing messages into an IMessageWriter for this socket.
                //消息处理器最终会将传出的消息推送到此套接字的IMessageWriter中。
                // 信息的处理,具体业务逻辑的实现在messageProcessor这个接口的实现中
                this.messageProcessor.process(message, this.writeProxy);
            }
            fullMessages.clear();
        }
        // 如果socket结束就关闭
        if (socket.endOfStreamReached) {
            System.out.println("Socket closed: " + socket.socketId);
            this.socketMap.remove(socket.socketId);
            key.attach(null);
            key.cancel();
            key.channel().close();
        }
    }


    public void writeToSockets() throws IOException {

        // Take all new messages from outboundMessageQueue
        // 从出栈消息队列中获取全部信息的消息
        takeNewOutboundMessages();

        // Cancel all sockets which have no more data to write.
        // 取消所有无法写入数据的套接字。
        cancelEmptySockets();

        // Register all sockets that *have* data and which are not yet registered.
        //注册所有有数据并且尚未注册的套接字。
        registerNonEmptySockets();

        // Select from the Selector.
        //从选择器中选择。
        //selectNow(),方法立即返回,不阻塞,如果没有返回0
        int writeReady = this.writeSelector.selectNow();
        // 如果大于0 说明有可写的链接
        if (writeReady > 0) {
            //获取链接的key
            Set<SelectionKey> selectionKeys = this.writeSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                //取出key中的附件(自定义Socket)
                Socket socket = (Socket) key.attachment();
                //
                socket.messageWriter.write(socket, this.writeByteBuffer);
                // 将没有信息需要写的Socket 加入 nonEmptyToEmptySockets 集合
                if (socket.messageWriter.isEmpty()) {
                    this.nonEmptyToEmptySockets.add(socket);
                }
                keyIterator.remove();
            }
            selectionKeys.clear();
        }
    }

    /**
     * 注册非空套接字到选择器,关注写事件
     *
     * @throws ClosedChannelException
     */
    private void registerNonEmptySockets() throws ClosedChannelException {
        for (Socket socket : emptyToNonEmptySockets) {
            socket.socketChannel.register(this.writeSelector, SelectionKey.OP_WRITE, socket);
        }
        emptyToNonEmptySockets.clear();
    }

    /**
     * 将 nonEmptyToEmptySockets 集合中的套接字 从选择器中取消
     */
    private void cancelEmptySockets() {
        for (Socket socket : nonEmptyToEmptySockets) {
            SelectionKey key = socket.socketChannel.keyFor(this.writeSelector);
            key.cancel();
        }
        nonEmptyToEmptySockets.clear();
    }

    private void takeNewOutboundMessages() {
        Message outMessage = this.outboundMessageQueue.poll();
        while (outMessage != null) {
            Socket socket = this.socketMap.get(outMessage.socketId);

            if (socket != null) {
                MessageWriter messageWriter = socket.messageWriter;
                if (messageWriter.isEmpty()) {
                    messageWriter.enqueue(outMessage);
                    nonEmptyToEmptySockets.remove(socket);
                    emptyToNonEmptySockets.add(socket);    //not necessary if removed from nonEmptyToEmptySockets in prev. statement.
                } else {
                    messageWriter.enqueue(outMessage);
                }
            }

            outMessage = this.outboundMessageQueue.poll();
        }
    }

}
