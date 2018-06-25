package com.jenkov.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 每个连接的读和写都由这里处理
 * Created by jjenkov on 16-10-2015.
 */
public class Socket {

    //连接的id
    public long socketId;
    //接收器获得的连接
    public SocketChannel socketChannel = null;
    //解析请求的对象
    public IMessageReader messageReader = null;
    //回写请求的对象
    public MessageWriter messageWriter = null;
    //到达流结束,读取结束的标志
    public boolean endOfStreamReached = false;

    public Socket() {}

    /**
     * 包装连接,存放在队列中
     *
     * @param socketChannel
     */
    public Socket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * 将数据读取到 byteBuffer,这个byteBuffer是外部传入的{@link SocketProcessor}
     * 具体的调用是{@link IMessageReader}
     *
     * @param byteBuffer
     * @return           具体读取到了多少信息,如果结束就返回-1
     * @throws IOException
     */
    public int read(ByteBuffer byteBuffer) throws IOException {
        // 当前读取到的位置
        int bytesRead = this.socketChannel.read(byteBuffer);
        // 读取到的字节数
        int totalBytesRead = bytesRead;
        // 假如读到 0 就是流还没有结束,但暂时没有信息传过来
        while (bytesRead > 0) {
            bytesRead = this.socketChannel.read(byteBuffer);
            //todo 读到 -1 还加吗?
            totalBytesRead += bytesRead;
        }
        // 如果返回-1就结束,后面没有信息了
        if (bytesRead == -1) {
            this.endOfStreamReached = true;
        }
        //返回读取的数量
        return totalBytesRead;
    }

    /**
     * 将缓存中的信息写入管道中
     *
     * @param byteBuffer
     * @return
     * @throws IOException
     */
    public int write(ByteBuffer byteBuffer) throws IOException {
        // 记录当次写了多少进管道
        int bytesWritten = this.socketChannel.write(byteBuffer);
        // 最终写了多少进管道
        int totalBytesWritten = bytesWritten;
        // 如果写入的数量大于0(能写入),并且缓存中还有信息就继续写入
        while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
            bytesWritten = this.socketChannel.write(byteBuffer);
            totalBytesWritten += bytesWritten;
        }
        // 返回写入的数量
        return totalBytesWritten;
    }


}
