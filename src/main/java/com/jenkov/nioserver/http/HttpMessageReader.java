package com.jenkov.nioserver.http;

import com.jenkov.nioserver.IMessageReader;
import com.jenkov.nioserver.Message;
import com.jenkov.nioserver.MessageBuffer;
import com.jenkov.nioserver.Socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jjenkov on 18-10-2015.
 */
public class HttpMessageReader implements IMessageReader {

    //信息的缓存
    private MessageBuffer messageBuffer = null;
    //完整数据
    private List<Message> completeMessages = new ArrayList<Message>();
    //下一个请求片段的信息
    private Message nextMessage = null;

    public HttpMessageReader() {
    }

    @Override
    public void init(MessageBuffer readMessageBuffer) {
        this.messageBuffer = readMessageBuffer;
        //从messageBuffer中获取新的message
        this.nextMessage = messageBuffer.getMessage();
        this.nextMessage.metaData = new HttpHeaders();
    }

    /**
     * 从{@link Socket}中读取信息,写入缓存
     * 支持多次读取,每次读取结束就将缓存清空
     *
     * @param socket
     * @param byteBuffer
     * @throws IOException
     */
    @Override
    public void read(Socket socket, ByteBuffer byteBuffer) throws IOException {
        // 读取信息写入缓存
        int bytesRead = socket.read(byteBuffer);
        // 转为读模式
        byteBuffer.flip();
        // remaining() 返回当前位置与限制之间的元素数目。
        // 如果没有读到数据
        if (byteBuffer.remaining() == 0) {
            // 清空缓存区
            byteBuffer.clear();
            return;
        }
        // 将读取到的数据写入message
        this.nextMessage.writeToMessage(byteBuffer);
        // 解析请求
        int endIndex = HttpUtil.parseHttpRequest(this.nextMessage.sharedArray, this.nextMessage.offset, this.nextMessage.offset + this.nextMessage.length, (HttpHeaders) this.nextMessage.metaData);
        //返回-1就是信息还没有传递结束,先不处理,等到信息全部传递过来再做处理
        if (endIndex != -1) {
            Message message = this.messageBuffer.getMessage();
            message.metaData = new HttpHeaders();
            // 将部分消息写入消息,将本次没有读完的信息(nextMessage)的片段写入message(下一个消息)
            message.writePartialMessageToMessage(nextMessage, endIndex);
            //将本次读到的信息加入集合
            completeMessages.add(nextMessage);
            // 将对象中的nextMessage指向当前创建的message
            nextMessage = message;
        }
        // 清空缓存区
        byteBuffer.clear();
    }


    @Override
    public List<Message> getMessages() {
        return this.completeMessages;
    }

}
