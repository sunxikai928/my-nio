package lean.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * 多人聊天室服务端
 * Created by sunxikai on 19/6/26.
 */
public class NioServer {

    private int port;

    // 多路复用器
    private Selector readSelector;
    /*
    Selector 多路复用器:
    cpu100%的bug 是因为底层使用了epoll命令,在linux中这个命令有时会返回0导致死循环;
    获取可执行的channel个数(不包含已经在集合中没有处理的数据):
    这两个方法都是返回 新加入 selectionKeys 集合的个数.当这个 selectionKey一直在集合中没有被删除,那么无论链接多少次,select返回的个数里都不包含它
    selectNow 直接返回,没有就是0;
    select : 当执行 wakeup 方法的时候,或者当前线程被打断都会返回,可能返回 0;

    ByteBuffer 缓存
    只能读一次,如果需要读多次请在开始加标记 mark 并在使用前 reset;
    读之前一定记得使用 flip 切换为读模式.完了以后记得clear清除数据准备再次使用

    ServerSocketChannel
    accept:如果设置了"非阻塞"模式,在没有连接请求的时候该接口会立即返回null; 在阻塞模式就等待;

    SocketChannel
    记得设置非阻塞模式;
    读写都是使用缓存;
    读:可以使用 Selector 有可读的数据时通知;
    写:只是将ByteBuffer写入了操作系统的缓存并没有实际发出去.真正发请求是操作系统做的.写出的时候无法得知是否成功了,(阻塞IO没有这个问题)

     */

    public NioServer(int port) {
        this.port = port;
        try {
            this.readSelector = Selector.open();
        } catch (IOException e) {
        }
    }

    /**
     * 功能:
     * 1.客户端能够注册;
     * 2.客户端能发送消息;
     * 3.客户端能接收到别的客户发的消息;
     *
     * @throws IOException
     */
    public void start() throws IOException {
        // 1.打开一个ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 2.绑定一个本地端口
        serverSocketChannel.bind(new InetSocketAddress(port));
        // 原本是想只生成一次
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode("您好!您与聊天室的其他人均不是好友请注意安全");
        byteBuffer.mark();
        for (; ; ) {
            // 3.接收连接请求
            /*
            accept:如果设置了"非阻塞"模式,在没有连接请求的时候该接口会立即返回null;
            */
            SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
                continue;
            }
            // 4.将channel设置为非阻塞
            socketChannel.configureBlocking(false);
            // 5.将获得的连接注册到Selector,并关注可读
            socketChannel.register(this.readSelector, SelectionKey.OP_READ);
            // 6.返回提示"您与聊天室的其他人均不是好友请注意安全"
            socketChannel.write(byteBuffer);
            byteBuffer.reset();
        }
    }

    /**
     * 处理读事件
     */
    private void read() {
        // 创建一个缓存
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        for (; ; ) {
            try {
            /*
            Selector 的 select : 当执行 wakeup 方法的时候,或者当前线程被打断都会返回
            返回的是准备就绪的集合 size;可能会是 0
             */
                int num = this.readSelector.selectNow();
                if (num > 0) {
                    Set<SelectionKey> selectionKeys = this.readSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        String request = "";
                        SelectionKey selectionKey = iterator.next();
                        // 使用完以后立即将其从集合中删除否则该对象会一直在集合中
                        iterator.remove();
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        // 将数据读入缓存, read 返回读取到的字节个数
                        int readSize = socketChannel.read(byteBuffer);
                        if (readSize > 0) {
                            // 将缓存切换为读模式
                            byteBuffer.flip();
                            request += Charset.forName("UTF-8").decode(byteBuffer);
                            // 置空缓存
                            byteBuffer.clear();
                            dis(selectionKey, request);
                        }
                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void dis(SelectionKey selectionKey, String request) throws IOException {
        Set<SelectionKey> allKeys = this.readSelector.keys();
        for (SelectionKey key : allKeys) {
            if (!selectionKey.equals(key)) {
                SocketChannel sc = (SocketChannel) key.channel();
                sc.write(Charset.forName("UTF-8").encode(request));
            }
        }
    }

    public static void main(String[] args) {
        final NioServer nioServer = new NioServer(8888);
        new Thread(() -> {
            try {
                nioServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            nioServer.read();
        }).start();
    }

}
