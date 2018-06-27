package sunxikai928.com.github;

/**
 * {@link FunctionalInterface} 函数式编程接口中只有一个方法,后续不可添加,可用于函数式编程
 * Created by jjenkov on 16-10-2015.
 */
@FunctionalInterface
public interface IMessageProcessor {

    void process(Socket socket);
}
