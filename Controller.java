import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class Controller implements Runnable {

    private static ServerSocket serverSocket;
    private Socket socket;
    private static Thread serverThread;
    private DataOutputStream outCommand;
    public static Integer totalConnections;

    private static ConcurrentHashMap<Integer, DataOutputStream> commandQueue = new ConcurrentHashMap<>(); 

    public Controller() {}
    public Controller(Socket s) {this.socket = s;}

    public void run() {
        // System.out.println("go run");
        try {
            outCommand = new DataOutputStream(socket.getOutputStream());
            String cidString = socket.getInetAddress().getHostName().toString().split("\\.")[0].substring(2,4);
            Integer cid = Integer.parseInt(cidString);
            System.out.println("Client: " + cid);
            // outCommand.writeUTF("hello");
            commandQueue.put(cid, this.outCommand);
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(9100);
            System.out.println("\033[1m\033[32mServer started: " + serverSocket.getLocalPort() + "\033[0m");
            while (true) {
                try {
                    final Socket receiveClientSocket = serverSocket.accept();
                    Thread commandThread = new Thread(new Controller(receiveClientSocket));
                    commandThread.start();
                    totalConnections++;
                } catch (IOException except) {
                    break;
                }
            }
            // serverSocket.close();
            return;
        } catch (IOException e) {
            System.err.println("Server creation failed !");
            e.printStackTrace();
        }
        System.out.println("\n\n\nEnding Server");
    }

    public static void sendUpdate(String inform, String message) throws Exception {
        String serverAddress = String.format("dc%02d.utdallas.edu", Integer.parseInt(inform));
        Socket sock = new Socket(serverAddress, 9050);
        DataOutputStream writeout = new DataOutputStream(sock.getOutputStream());
        writeout.writeUTF(message);
        sock.close();
    }

    public static void sendCommand(String command) throws Exception {

        if (command.split(" ").length != 3) {
            System.out.println("Illegal command");
            return;
        }

        String action = command.split(" ")[0];
        String inform = command.split(" ")[1];
        String about = command.split(" ")[2];

        if (action.equals("break")) {

            for (int i = 0; i < inform.length(); i++) {
                DataOutputStream out =  commandQueue.get(Integer.parseInt(inform.substring(i,i+1)));
                out.writeUTF(action+" "+about);
            }

            inform = command.split(" ")[2];
            about = command.split(" ")[1];

            for (int i = 0; i < inform.length(); i++) {
                DataOutputStream out =  commandQueue.get(Integer.parseInt(inform.substring(i,i+1)));
                out.writeUTF(action+" "+about);
            }

        } else if (action.equals("merge")) {
            for (int i = 0; i < inform.length(); i++) {
                DataOutputStream out =  commandQueue.get(Integer.parseInt(inform.substring(i,i+1)));
                out.writeUTF(action+" "+about);
            }
        } else {
            sendUpdate(inform, about);
        }

        
        // out =  commandQueue.get(2);
        // out.writeUTF(command);
    }

    public static void main(String[] args) throws Exception {

        totalConnections = 0;

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new Controller().startServer();
            }
        });
        serverThread.start();

        Scanner consoleRead = new Scanner(System.in);
        while (totalConnections < 8) {
            TimeUnit.MICROSECONDS.sleep(1);
        }
            
        while (true) {
            System.out.println("\033[1m\033[33m\nCommand:\033[0m");
            String readCommand = consoleRead.nextLine();
            sendCommand(readCommand);
            System.out.println("Sent command successfully: " + readCommand);
        }
        

    }

}