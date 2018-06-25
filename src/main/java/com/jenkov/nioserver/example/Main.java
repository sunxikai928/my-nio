package com.jenkov.nioserver.example;

import com.jenkov.nioserver.IMessageProcessor;
import com.jenkov.nioserver.Message;
import com.jenkov.nioserver.Server;
import com.jenkov.nioserver.http.HttpMessageReaderFactory;

import java.io.IOException;

/**
 * 启动后浏览器访问 http://localhost:9999/
 * Created by jjenkov on 19-10-2015.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        /**
         * 返回的消息
         */
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 38\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                "<html><body>Hello World!</body></html>";
        // 转为字节数组
        byte[] httpResponseBytes = httpResponse.getBytes("UTF-8");

        /**
         * 消息处理对象
         */
        IMessageProcessor messageProcessor = (request, writeProxy) -> {
            //从套接字接收的消息
            System.out.println("Message Received from socket: " + request.socketId);
            // 创建返回值 response
            Message response = writeProxy.getMessage();
            // response 和 request 的 socketId 是同一个
            response.socketId = request.socketId;
            //写入需要返回的信息
            response.writeToMessage(httpResponseBytes);
            // 将消息加入出站消息队列
            writeProxy.enqueue(response);
        };

        Server server = new Server(9999, new HttpMessageReaderFactory(), messageProcessor);

        server.start();

    }


}
