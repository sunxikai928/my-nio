package lean.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * 多人聊天室客户端
 * Created by sunxikai on 19/6/26.
 */
public class NioClient {

    private Selector readSelector;
    private String name;
    private String host;
    private int port;
    private SocketChannel socketChannel;
    Scanner scanner = new Scanner(System.in);

    public NioClient(String name, String host, int port) throws IOException {
        this.name = name;
        this.host = host;
        this.port = port;
        try {
            this.readSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        init();
    }

    public void init() throws IOException {
        // 打开 SocketChannel 获取连接
        socketChannel = SocketChannel.open(new InetSocketAddress(this.host, this.port));
        // 设置连接为非阻塞
        socketChannel.configureBlocking(false);
        // 注册连接到 读选择器
        socketChannel.register(this.readSelector, SelectionKey.OP_READ);
    }

    public void read() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        for (; ; ) {
            // 获取已经准备好的链接个数
            int num = this.readSelector.selectNow();
            System.out.println(num);
            System.out.println(this.readSelector.selectNow());
            if (num > 0) {
                Set<SelectionKey> selectionKeys = this.readSelector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    /*
                     一定记得从 selectionKeys 集合中删除.
                     问题现象:
                     消息发出后到达服务端,服务端正常回写但是客户端 int num = this.readSelector.selectNow();一直返回0
                     原因就是 selectionKey 没有从集合中删除,后面有了新的事件,选择器就不再提醒了.
                      */
                    iterator.remove();
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    int readSize = socketChannel.read(byteBuffer);
                    if (readSize > 0) {
                        // 将缓存切换为读模式
                        byteBuffer.flip();
                        System.out.println(Charset.forName("UTF-8").decode(byteBuffer));
                    }
                    byteBuffer.clear();
                }
            } else {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void writ() throws IOException {
        for (; ; ) {
            String request = getRequest();
            socketChannel.write(Charset.forName("UTF-8").encode(request));
        }
    }

    public String getRequest() {
        String request = "";
        // 获取标准输入流
        if (scanner.hasNext()) {
            request = scanner.nextLine();
        }
        return this.name + ": " + request;
    }

    public static void main(String[] args) throws IOException {
        NioClient client = new NioClient("C", "localhost", 8888);
        new Thread(() -> {
            try {
                client.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                client.writ();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
