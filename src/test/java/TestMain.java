import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by sunxikai on 18/6/24.
 */
public class TestMain {

    @Test
    public void test() throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 10000));
        ByteBuffer readBuff = ByteBuffer.allocate(1024);
        ByteBuffer writeBuff = ByteBuffer.allocate(1024);

        int i = 1;

        for (; ; ) {
            String s = "客户端第 " + i++ + "次请求.";
            writeBuff.put(s.getBytes("UTF-8"));
            writeBuff.flip();
            socketChannel.write(writeBuff);
            System.out.println("客户端发出的信息:" + s);
            int n = socketChannel.read(readBuff);
            readBuff.flip();
            System.out.println("客户端收到的信息:" + new String(readBuff.array(), 0, n));
            writeBuff.clear();
            readBuff.clear();
            Thread.sleep(1000);
            if (i == 15){
                socketChannel.close();
                break;
            }
        }
    }
}
