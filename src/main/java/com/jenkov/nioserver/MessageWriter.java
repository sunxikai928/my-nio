package com.jenkov.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jjenkov on 21-10-2015.
 */
public class MessageWriter {
    //有多个需要写的信息,从第二个开始
    private List<Message> writeQueue = new ArrayList<>();
    //当前需要写的信息
    private Message messageInProgress = null;
    //已经写入的信息字节数
    private int bytesWritten = 0;

    public MessageWriter() {
    }

    public void enqueue(Message message) {
        if (this.messageInProgress == null) {
            this.messageInProgress = message;
        } else {
            this.writeQueue.add(message);
        }
    }

    public void write(Socket socket, ByteBuffer byteBuffer) throws IOException {
        // 将 messageInProgress.sharedArray 中的信息写入缓存 byteBuffer(公用缓存)
        byteBuffer.put(this.messageInProgress.sharedArray, this.messageInProgress.offset + this.bytesWritten, this.messageInProgress.length - this.bytesWritten);
        //转换为读模式
        byteBuffer.flip();
        //将缓存中的数据写入连接,返回写入数量, bytesWritten 记录当前已经写入的数量
        this.bytesWritten += socket.write(byteBuffer);
        // 清空缓存
        byteBuffer.clear();
        //写入数量 大于等于 信息长度
        if (bytesWritten >= this.messageInProgress.length) {
            //看集合中是否还有下一个信息,如果有下一个信息 就下次循环到这里的时候再继续写
            if (this.writeQueue.size() > 0) {
                this.messageInProgress = this.writeQueue.remove(0);
            } else {
                //如果集合中没有信息了,将信息置空
                this.messageInProgress = null;
                //todo unregister from selector
            }
        }
    }

    /**
     * 是否没有信息,集合中是空的,并且 messageInProgress 也是空的
     *
     * @return
     */
    public boolean isEmpty() {
        return this.writeQueue.isEmpty() && this.messageInProgress == null;
    }

}
