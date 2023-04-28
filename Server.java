import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.io.*;

public class Server implements Runnable {

    public static ConcurrentHashMap<Integer, Socket> socketMap = new ConcurrentHashMap<>(); 
    public static HashMap<String, Integer> fileStatus = new HashMap<>();
    public static String fileData = "";

    public void run() {
        long currentThread = Thread.currentThread().getId();
        System.out.println("Thread: \033[1m\033[32m" + currentThread + "\033[0m running file server");
        try {
            ServerSocket serverSocket = new ServerSocket(9050);
            System.out.println("\033[1m\033[32mFile Server on: " + serverSocket.getLocalPort() + "\033[0m");
            // Accept and manage clients
            while (true) {
                try {
                    Socket receiveWriterSocket = serverSocket.accept();
                    String clientAddress = receiveWriterSocket.getInetAddress().getHostName().toString().split("\\.")[0];
                    DataInputStream readIn = new DataInputStream(receiveWriterSocket.getInputStream());
                    String dataReceive = readIn.readUTF();
                    System.out.println("Got data: " + dataReceive);
                } catch (IOException except) {
                    break;
                }
            }
            serverSocket.close();
            return;
        } catch (IOException e) {
            if (true) {
                System.err.println("Server creation failed !");
                e.printStackTrace();
            }
        }
        System.out.println("\n\n\nEnding Server");
    }

    public static void main(String[] args) throws Exception {

        ConnectionHandler connections = new ConnectionHandler(socketMap);
        Thread connectionHandler = new Thread(connections);
        connectionHandler.start();
        
        Server fileServer = new Server();
        Thread fileServerThread = new Thread(fileServer);
        fileServerThread.start();


        connectionHandler.join();
        System.out.println("running main");
    }
    
}