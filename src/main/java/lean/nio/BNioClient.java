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
public class BNioClient {

    private Selector readSelector;
    private String name;
    private String host;
    private int port;
    private SocketChannel socketChannel;
    Scanner scanner = new Scanner(System.in);

    public BNioClient(String name, String host, int port) throws IOException {
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
            if (num > 0) {
                Set<SelectionKey> selectionKeys = this.readSelector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
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
                    Thread.sleep(10);
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
        BNioClient client = new BNioClient("B", "localhost", 8888);
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
