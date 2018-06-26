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
 * 读取选择器的选中条件是有数据准备好,可读.
 * 具体是 操作系统已经接收到了完整的包(可有多个TCP包).才会通知应用程序可读,这时候数据是已经在操作系统的读缓存中
 * FIXME 有个疑问就是如果一个 http 请求比较大,分了多个TCP包,操作系统在未拿到所有TCP包的时候会通知应用程序可读吗?
 * 我猜想是不会,操作系统不仅仅是拿到包就完了,还会对TCP包进行排序,去除TCP头信息,还原到数据发送时的状态(完整的包),
 * 这都要所有数据包都存在才能操作,不知道这个猜想对不对
 * <p>
 * FIXME 对于数据的解析(其实就是具体协议)以及处理这块我还没想好怎么做,具体信息的处理肯定是在应用程序中
 * 可以定义一个应用处理的角色,接收参数并返回回写信息,具体的
 * Created by sunxikai on 18/6/24.
 */
public class SocketRead implements Runnable {
    private static Logger log = LoggerFactory.getLogger(SocketRead.class);
    // 读选择器
    public Selector readSelector = null;
    // 写
    private SocketWrite socketWrite;
    //读的缓存
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);
    // 应用外部处理角色
    private IMessageProcessor iMessageProcessor;

    public SocketRead(SocketWrite socketWrite, IMessageProcessor iMessageProcessor) throws IOException {
        this.readSelector = Selector.open();
        this.socketWrite = socketWrite;
        this.iMessageProcessor = iMessageProcessor;
    }

    /**
     * 将信息读取并解析完成后加入到队列中让应用程序消费
     */
    public void run() {
        for (; ; ) {
            try {
                int num = this.readSelector.selectNow();
                if (num > 0) {
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
                        socket.requestMessage = new String(this.readBuffer.array(), 0, i);

                        // 处理数据 解析/处理返回值
                        this.iMessageProcessor.process(socket);

                        // 不注册 注册的过程移动到写线程中取,这里加入队列
                        socketWrite.queue.offer(socket);

                        // 将 selectionKey 从已选中中删除
                        iterator.remove();
                        this.readBuffer.clear();
                    }
                    selectionKeys.clear();
                } else {
                    // 没有可读的时候休眠
                    // 休眠是必须的,如果不休眠CPU会100%
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                log.error("获取可读渠道报错", e);
            }

        }
    }
}
