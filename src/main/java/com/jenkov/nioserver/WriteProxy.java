package com.jenkov.nioserver;

import java.util.Queue;

/**
 * Created by jjenkov on 22-10-2015.
 */
public class WriteProxy {
    //回写的缓存
    private MessageBuffer messageBuffer = null;
    // 回写消息队列,就是出栈消息队列
    private Queue        writeQueue     = null;

    public WriteProxy(MessageBuffer messageBuffer, Queue writeQueue) {
        this.messageBuffer = messageBuffer;
        this.writeQueue = writeQueue;
    }

    public Message getMessage(){
        return this.messageBuffer.getMessage();
    }
    //将消息加入出栈消息队列,等候处理
    public boolean enqueue(Message message){
        return this.writeQueue.offer(message);
    }

}
