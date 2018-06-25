package sunxikai928.com.github;

import java.nio.channels.SocketChannel;

/**
 * 包含 Channel/request/response/reader/writer/selectionKey
 * Created by sunxikai on 18/6/23.
 */
public class Socket {

    public SocketChannel socketChannel;
    public String message;

    public Socket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public String toString() {
        return socketChannel.socket().toString();
    }
}
