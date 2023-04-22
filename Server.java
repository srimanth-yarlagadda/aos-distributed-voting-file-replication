import java.net.*;
import java.util.concurrent.*;

public class Server implements Runnable {

    public static ConcurrentHashMap<Integer, Socket> socketMap = new ConcurrentHashMap<>(); 

    public void run() {
        //
    }

    public static void main(String[] args) throws Exception {

        ConnectionHandler connections = new ConnectionHandler(socketMap);
        Thread connectionHandler = new Thread(connections);
        connectionHandler.start();
        connectionHandler.join();
        
        System.out.println("running main");
    }
    
}