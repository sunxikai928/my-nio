package sunxikai928.com.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 监听数据的读取
 * Created by sunxikai on 18/6/24.
 */
public class SocketRead implements Runnable {
    private static Logger log = LoggerFactory.getLogger(SocketRead.class);
    // 读选择器
    public Selector readSelector = null;

    private SocketWrite socketWrite;
    //读的缓存
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);

    public SocketRead(SocketWrite socketWrite) throws IOException {
        this.readSelector = Selector.open();
        this.socketWrite = socketWrite;
    }

    /**
     * 将信息读取并解析完成后加入到队列中让应用程序消费
     */
    public void run() {
        for (; ; ) {
            try {
                int num = this.readSelector.selectNow();
                while (num > 0) {
                    num = 0;
                    Set<SelectionKey> selectionKeys = this.readSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        Socket socket = (Socket) selectionKey.attachment();
                        SocketChannel socketChannel = socket.socketChannel;
                        int i = socketChannel.read(this.readBuffer);
                        if (i == -1) {
                            // 这里就是这个流结束了
                            selectionKey.attach(null);
                            selectionKey.cancel();
                            selectionKey.channel().close();
                            iterator.remove();
                            this.readBuffer.clear();
                            continue;
                        }
                        this.readBuffer.flip();

                        // 处理数据 解析/处理返回值
                        String message = new String(this.readBuffer.array(), 0, i);
                        System.out.println(message);
                        socket.message = "收到信息:" + message;
                        // 这里的注册要和SocketWrite中的取消同步
                        socketChannel.register(socketWrite.writSelector, SelectionKey.OP_WRITE, socket);

                        // 将 selectionKey 从已选中中删除
                        iterator.remove();
                        this.readBuffer.clear();
                    }
                    selectionKeys.clear();
                }
            } catch (IOException e) {
                log.error("获取可读渠道报错", e);
            }

        }
    }
}
