import java.net.*;
import java.io.*;
import java.util.*;

public class ConnectionHandler implements Runnable {

    private String serverID;
    private int myPort;
    ServerSocket serverSocket;

    Thread serverThread;

    private boolean systemDebug = true;
    private boolean runServerBoolean = true;

    public ConnectionHandler(int port, String serverID) {
        this.myPort = port;
        this.serverID = serverID;
    }

    public ConnectionHandler() {}

    public void startServer() {
        long currentThread = Thread.currentThread().getId();
        System.out.println("Thread: \033[1m\033[32m" + currentThread + "\033[0m running Server");
        try {
            serverSocket = new ServerSocket(myPort);
            System.out.println("\033[1m\033[32mServer started: " + serverSocket.getLocalPort() + "\033[0m");
            // Accept and manage clients
            while (runServerBoolean) {
                try {
                    final Socket receiveClientSocket = serverSocket.accept();
                    // String clientAddress = receiveClientSocket.getInetAddress().getHostName().toString().split("\\.")[0];
                    // final int clientID = Integer.parseInt(clientAddress.substring(2,4));
                    final PeerHandler peer = new PeerHandler();
                    peer.assignCommunicationSocket(receiveClientSocket);
                    Thread peerThread = new Thread(peer);
                    peerThread.start();
                } catch (IOException except) {
                    break;
                }
            }
            serverSocket.close();
            return;
        } catch (IOException e) {
            if (systemDebug) {
                System.err.println("Server creation failed !");
                e.printStackTrace();
            }
        }
        System.out.println("\n\n\nEnding Server");
    }
    
    public List<String> generateIPAddresses() {
        Set<String> serverList = new HashSet<String>();
        for (int serverID = 1; serverID <= 3; serverID++) {
            serverList.add(String.format("dc%02d.utdallas.edu:%d", serverID, 9037 + serverID));
            serverList.add(String.format("dc%02d.utdallas.edu:%d", (serverID*2), 9037 + (serverID*2)));
            serverList.add(String.format("dc%02d.utdallas.edu:%d", (serverID*2)+1, 9037 + (serverID*2) +1));
        }
        return new ArrayList<String>(serverList);
    }

    
    public void generateIdentity() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            serverID = localAddress.toString().split("\\.")[0];
            myPort = 9037 + Integer.parseInt(serverID.substring(2,4));
            System.out.println("Server | Port ===>   " + serverID + " | " + myPort);
            if (Integer.parseInt(serverID.substring(2,4)) >= 4) {
                int activeClients = 0;
            }
        } catch (UnknownHostException except) {
            System.err.println("Host Unknown");
            except.printStackTrace();
        }
    }

    public void server() {
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new ConnectionHandler(myPort, serverID).startServer();
            }
        });
        serverThread.start();
    }

    public void connectController() {
        Thread controllerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket controllerSocket = new Socket("dc10.utdallas.edu", 9100);
                    System.out.println(".... controller connected");
                    DataInputStream inCommand = new DataInputStream(controllerSocket.getInputStream());
                    String s = inCommand.readUTF();
                    System.out.println("controller says: " + s);
                    // break;
                } catch (IOException exc) {
                    // try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException e) {e.printStackTrace();}
                    exc.printStackTrace();
                    System.out.println("retrying connection ....");
                }
            }
        });
        controllerThread.start();
    }


    public void connectToPeers() {
        List<String> ips = generateIPAddresses();
        for (String addr: ips) {
            String serverIP = addr.split(":")[0];
            String serverPort = addr.split(":")[1];
            if (Integer.parseInt(serverIP.substring(2,4)) < Integer.parseInt(serverID.substring(2,4))) {
                try {
                    final Socket peerSocket = new Socket(serverIP, Integer.parseInt(serverPort));
                    PeerHandler peer = new PeerHandler();
                    peer.assignCommunicationSocket(peerSocket);
                    Thread peerThread = new Thread(peer);
                    peerThread.start();
                    System.out.println(".... connected");
                    // break;
                } catch (IOException exc) {
                    // try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException e) {e.printStackTrace();}
                    exc.printStackTrace();
                    System.out.println("retrying connection ....");
                }
            }
        }
    }


    public void run() {
        generateIdentity();
        System.out.println("Assigned prot : " + myPort);
        server();
        connectController();
        
        connectToPeers();
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("run method of connection");
    }
       
}
