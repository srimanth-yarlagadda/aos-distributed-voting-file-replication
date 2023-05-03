import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ConnectionHandler implements Runnable {

    public String serverID;
    private int myPort;
    
    ServerSocket serverSocket;
    public ConcurrentHashMap<Integer, Socket> socketMap;
    public ConcurrentHashMap<Integer, PeerHandler> peerMap = new ConcurrentHashMap<>();
    public Server fileServer;

    Thread serverThread;

    private boolean systemDebug = true;
    private boolean runServerBoolean = true;

    private ConnectionHandler parentHandler;

    public ConnectionHandler(int port, String serverID, ConnectionHandler handler) {
        this.myPort = port;
        this.serverID = serverID;
        this.parentHandler = handler;
    }

    public ConnectionHandler(ConcurrentHashMap<Integer, Socket> s) {
        this.socketMap = s;
    }

    public ConnectionHandler() {}

    public void startServer(ConcurrentHashMap<Integer, Socket> socketMap) {
        long currentThread = Thread.currentThread().getId();
        System.out.println("Thread: \033[1m\033[32m" + currentThread + "\033[0m running Server");
        try {
            serverSocket = new ServerSocket(myPort);
            System.out.println("\033[1m\033[32mServer started: " + serverSocket.getLocalPort() + "\033[0m");
            // Accept and manage clients
            while (runServerBoolean) {
                try {
                    final Socket receiveClientSocket = serverSocket.accept();
                    String clientAddress = receiveClientSocket.getInetAddress().getHostName().toString().split("\\.")[0];
                    // final int clientID = Integer.parseInt(clientAddress.substring(2,4));
                    final PeerHandler peer = new PeerHandler(parentHandler.fileServer);
                    peer.assignCommunicationSocket(receiveClientSocket);
                    // System.out.println("GOTTT: " + clientAddress);
                    parentHandler.socketMap.put(Integer.parseInt(clientAddress.substring(2,4)), receiveClientSocket);
                    parentHandler.peerMap.put(Integer.parseInt(clientAddress.substring(2,4)), peer);
                    // System.out.println(peerMap);
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

    public String generateIPfor(String serverID) {
        Integer sid = Integer.parseInt(serverID);
        return String.format("dc%02d.utdallas.edu:%d", sid, 9037 + sid);
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
        final ConnectionHandler currentInstance = this;
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new ConnectionHandler(myPort, serverID, currentInstance).startServer(socketMap);
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
                    System.out.println("\033[1m\033[33mController Connected\033[0m");
                    DataInputStream inCommand = new DataInputStream(controllerSocket.getInputStream());
                    while (true) {
                        String s = inCommand.readUTF();
                        System.out.println("[Socket] Command: " + s);
                        String action = s.split(" ")[0];
                        String about = s.split(" ")[1];
                        for (int i = 0; i < about.length(); i++) {
                            // System.out.println(about.substring(i,i+1));
                            if (action.equals("break")) {
                                // System.out.println("socket list : " + socketMap.toString());
                                breakPeerConnection(Integer.parseInt(about.substring(i,i+1)));
                            } else if (action.equals("merge")) {                               
                                String nodeDetails = generateIPfor(about.substring(i,i+1));
                                makePeerConnection(nodeDetails.split(":")[0],nodeDetails.split(":")[1]);
                            } 
                        }
                        System.out.println("");
                        // System.out.println(socketMap);
                    }
                } catch (IOException exc) {
                    // try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException e) {e.printStackTrace();}
                    exc.printStackTrace();
                    System.out.println("retrying connection ....");
                }
            }
        });
        controllerThread.start();
    }

    public void breakPeerConnection(Integer peerID) {
        Socket socket = socketMap.get(peerID);
        try {
            System.out.println("Close: " + socket.getInetAddress());
            socket.close();
        } catch (IOException e) {e.printStackTrace();}
        socketMap.remove(peerID);
        peerMap.remove(peerID);
    }

    public void makePeerConnection(String serverIP, String serverPort) {
        Integer tries = 5;
        while (tries > 0) {
            try {
                final Socket peerSocket = new Socket(serverIP, Integer.parseInt(serverPort));
                PeerHandler peer = new PeerHandler(fileServer);
                peer.assignCommunicationSocket(peerSocket);
                Thread peerThread = new Thread(peer);
                peerThread.start();
                socketMap.put(Integer.parseInt(serverIP.substring(2,4)), peerSocket);
                peerMap.put(Integer.parseInt(serverIP.substring(2,4)), peer);
                tries = 0; break;
                // System.out.println(peerMap);
                // if (fileServer.getDSstatus()) {
                //     try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException e) {}
                //    peer.askToSync(String.format("-%d %d %d " + fileServer.fileData, fileServer.fileStatus.get("VN"), fileServer.fileStatus.get("RU"), fileServer.fileStatus.get("DS")));
                // }
                // System.out.println(".... connected");
            } catch (IOException exc) {
                try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException e) {e.printStackTrace();}
                // exc.printStackTrace();
                System.out.println("\tretrying connection .... " + serverIP); tries--;
            }
        }
    }

    public void connectToPeers() {
        List<String> ips = generateIPAddresses();
        for (String addr: ips) {
            String serverIP = addr.split(":")[0];
            String serverPort = addr.split(":")[1];
            if (Integer.parseInt(serverIP.substring(2,4)) < Integer.parseInt(serverID.substring(2,4))) {
                makePeerConnection(serverIP, serverPort);
            }
        }
    }


    public void run() {
        generateIdentity();
        System.out.println("Assigned port : " + myPort);
        server();
        connectController();
        
        fileServer = new Server(this);
        Thread fileServerThread = new Thread(fileServer);
        fileServerThread.start();

        connectToPeers();
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("run method of connection");
    }
       
}
