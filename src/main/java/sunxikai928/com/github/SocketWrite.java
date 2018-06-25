package sunxikai928.com.github;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * 监听数据的写出
 * Created by sunxikai on 18/6/24.
 */
public class SocketWrite implements Runnable {

    // 写选择器
    public Selector writSelector = null;

    //读的缓存
    private ByteBuffer writBuffer = ByteBuffer.allocate(1024 * 1024);

    public SocketWrite() throws IOException {
        this.writSelector = Selector.open();
    }

    public void run() {
        for (; ; ) {
            try {
                // selectNow 立即返回 不阻塞 可能为 0
                int num = this.writSelector.selectNow();
                while (num > 0) {
                    num = 0;
                    Set<SelectionKey> selectionKeys = this.writSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while(iterator.hasNext()){
                        SelectionKey selectionKey = iterator.next();
                        Socket socket = (Socket) selectionKey.attachment();
                        if (socket.message != null){
                            this.writBuffer.put(socket.message.getBytes("UTF-8"));
                            // 转换为读模式
                            this.writBuffer.flip();
                            socket.socketChannel.write(this.writBuffer);

                            socket.message = null;
                            this.writBuffer.clear();
                        }
                        iterator.remove();
                        // selectionKey 的取消和注册 需要进行处理
                        // 如果注册过程中另一个线程调用了cancel()
                        // 会报异常 java.nio.channels.CancelledKeyException
                        // SocketRead 中线程注册未完成结果这边取消了 所以当同一个连接请求密集可能会报错
                        selectionKey.cancel();
                    }
                    selectionKeys.clear();
                }
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
}
