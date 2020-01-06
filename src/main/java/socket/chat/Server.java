package socket.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8089);
        System.out.println("开启服务器，等待客户端连接诶...");
        Socket socket = serverSocket.accept();
        System.out.println("客户端已连接");
        new ReceiveThread("Client",socket).start();
        new SendThread("Server",socket).start();
}
}
