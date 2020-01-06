package socket.chat;

import java.io.IOException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8089);
        System.out.println("成功连接服务器....");
        new ReceiveThread("Server",socket).start();
        new SendThread("Client",socket).start();
    }
}
