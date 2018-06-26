package sunxikai928.com.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 监听数据的写出
 * 选择器中可写的判断条件是,操作系统的写缓存区有空闲空间(基本一直有空闲空间)
 * 所以只要是注册了的 channel 一直可写,这些key 在被取消之前会一直被返回
 * 如果取消 注册,只是被放入了删除队列中,直到进行下一次选择操作时才移除该键.
 * 如果是被放入了删除队列,这时候channel再次注册就会报错 java.nio.channels.CancelledKeyException
 * 下一次选择操作我看了一下就是有新的连接进来被选中
 * 所以这里是加了一个队列,只有有消息要写的时候我才去遍历选择器,没有东西写就睡眠
 * Created by sunxikai on 18/6/24.
 */
public class SocketWrite implements Runnable {
    private static Logger log = LoggerFactory.getLogger(SocketWrite.class);

    // 写选择器
    public Selector writSelector = null;
    // 队列
    public Queue<Socket> queue = new ArrayBlockingQueue<Socket>(1024 * 1024);

    //读的缓存
    private ByteBuffer writBuffer = ByteBuffer.allocate(1024 * 1024);

    public SocketWrite() throws IOException {
        this.writSelector = Selector.open();
    }

    public void run() {
        for (; ; ) {
            if (register() == 0) {
                // 没有需要写的时候休眠
                // 休眠是必须的,如果不休眠CPU会100%
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            write();

        }
    }

    // 从队列中取值并注册到写选择器中
    private int register() {
        Socket socket = null;
        int i = 0;
        //从队列中拿值,有
        while ((socket = this.queue.poll()) != null) {
            try {
                //没有信息要写的话也不要注册
                if (socket.message == null) {
                    continue;
                }
                //如果已经注册过了就不用注册了
                if (socket.socketChannel.keyFor(this.writSelector) == null) {
                    socket.socketChannel.register(this.writSelector, SelectionKey.OP_WRITE, socket);
                }
                i++;
            } catch (ClosedChannelException e) {
                log.error("将数据注册到选择器中的时候报错啦", e);
            }
        }
        return i;
    }

    private void write() {
        try {
            // selectNow 立即返回 不阻塞 可能为 0
            // 判断条件是操作系统的 写缓存区域有空间就返回 基本上 写缓存区 一直有空闲空间
            // 所以只要是注册在这个选择器上的都会被认为一直都是准备好的
            // 除非将这个 key 取消
            int num = this.writSelector.selectNow();
            if (num > 0) {
                // selectedKeys 返回已经准备好的 key
                Set<SelectionKey> selectionKeys = this.writSelector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    Socket socket = (Socket) selectionKey.attachment();
                    if (socket.message != null) {
                        this.writBuffer.put(socket.message.getBytes("UTF-8"));
                        // 转换为读模式
                        this.writBuffer.flip();
                        socket.socketChannel.write(this.writBuffer);
                        socket.message = null;
                        this.writBuffer.clear();
                    }
                    // selectionKey 取消之后不能再在这个选择器中注册了
                    // 会报异常 java.nio.channels.CancelledKeyException
                    /*
                    请求取消此键的通道到其选择器的注册。一旦返回，该键就是无效的，
                    并且将被添加到其选择器的已取消键集中。
                    在进行下一次选择操作时，将从所有选择器的键集中移除该键。
                     */
                    // 这里的下一次操作是 有新的进线 选择器又重新选择了一遍
                    // 关闭连接的时候会自动取消,这里不再取消, 只要连接存在就一直监听着
                    // selectionKey.cancel();
                    iterator.remove();
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            log.error("回写数据的时候报错了", e);
        }
    }
}
