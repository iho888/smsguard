import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.net.InetSocketAddress;

public class SelectorTest {
    public static void main(String[] args) throws Exception {
        System.out.println("java.version=" + System.getProperty("java.version"));
        System.out.println("Opening Selector...");
        Selector s = Selector.open();
        System.out.println("Selector class=" + s.getClass().getName());

        System.out.println("Binding loopback ServerSocket...");
        ServerSocketChannel ch = ServerSocketChannel.open();
        ch.configureBlocking(false);
        ch.bind(new InetSocketAddress("127.0.0.1", 0));
        ch.register(s, java.nio.channels.SelectionKey.OP_ACCEPT);
        System.out.println("Bound to " + ch.getLocalAddress());

        ch.close();
        s.close();
        System.out.println("OK");
    }
}
