import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.io.*;
import java.lang.Math;

public class Server implements Runnable {

    public static ConcurrentHashMap<Integer, Socket> socketMap = new ConcurrentHashMap<>();
    public static HashMap<String, Integer> fileStatus = new HashMap<>();
    public static HashMap<Integer, Integer> fileUpdateWithPeers = new HashMap<>();
    public static HashMap<Integer, String> fileUpdateLog = new HashMap<>();
    public static String fileData = "";

    private ConnectionHandler handler;
    public Integer myID;

    public Server(ConnectionHandler h) {
        this.handler = h;
        this.myID = Integer.parseInt(h.serverID.substring(2,4));
        // Initialize Algorithm Parameters
        fileStatus.put("VN",0);
        fileStatus.put("RU",8);
        fileStatus.put("DS",0);
        for (int i = 1; i <= 8; i++) {
            fileUpdateWithPeers.put(i,0);
        }
    }

    public void printFileStatus() {
        // System.out.println("\033[1m\033[33m");
        // System.out.println("\u001B[36m");
        System.out.println("\033[1;36m");
        System.out.println(String.format("Version: %d, Replicas: %d, Distinguished Site: %d\nData: "+fileData, 
            fileStatus.get("VN"), fileStatus.get("RU"), fileStatus.get("DS")));
        System.out.println("\033[0m");
        // System.out.println("\u001B[0m");
    }

    public void debugStats() {
        System.out.println("\033[33m\tINFO");
        System.out.println("\t"+fileUpdateWithPeers);
        System.out.println("\t"+fileUpdateLog);
        System.out.println("\t"+String.format("Version: %d, Replicas: %d, Distinguished Site: %d\nData: "+fileData, 
            fileStatus.get("VN"), fileStatus.get("RU"), fileStatus.get("DS")));
        // printFileStatus();
        System.out.println("\033[0m");

    }

    public boolean getDSstatus() {
        ArrayList<Integer> currentSet = new ArrayList<>(handler.peerMap.keySet());
        currentSet.add(myID);
        if (currentSet.contains(fileStatus.get("DS"))) {
            return true;
        }
        return false;
    }

    public void updatePeers(String message) {
        Integer peersInPartition = handler.peerMap.size();
        System.out.println("Total in partition: " + peersInPartition);
        ArrayList<Integer> currentSet = new ArrayList<>(handler.peerMap.keySet());
        currentSet.add(myID);
        
        if ((peersInPartition+1) > (fileStatus.get("RU")/2)) {
            fileStatus.put("VN", fileStatus.get("VN") + 1);
            fileStatus.put("RU", peersInPartition+1);
            fileStatus.put("DS", Collections.min(currentSet));
            fileUpdateLog.put(fileStatus.get("VN"), message);
            if (fileData.equals("")) {
                fileData = message;
            } else {
                fileData = fileData + " " + message;
            }
            for (Integer p: handler.peerMap.keySet()) {
                // System.out.println("\tDOING " + p);
                (handler.peerMap.get(p)).askToUpdate(String.format("%d %d %d " + message, fileStatus.get("VN"), fileStatus.get("RU"), fileStatus.get("DS")));
                fileUpdateWithPeers.put(p, fileStatus.get("VN"));
            }
        } else if (((peersInPartition+1) == (fileStatus.get("RU")/2)) && (currentSet.contains(fileStatus.get("DS")))) {
            fileStatus.put("VN", fileStatus.get("VN") + 1);
            fileStatus.put("RU", peersInPartition+1);
            fileUpdateLog.put(fileStatus.get("VN"), message);
            fileData = fileData + " " + message;
            for (Integer p: handler.peerMap.keySet()) {
                (handler.peerMap.get(p)).askToUpdate(String.format("%d %d %d " + message, fileStatus.get("VN"), fileStatus.get("RU"), fileStatus.get("DS")));
                fileUpdateWithPeers.put(p, fileStatus.get("VN"));
            }
        } else {
            System.out.println("\u001B[31mFile not updated !\u001B[0m");
        }

        printFileStatus();
    }

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
                    if (dataReceive.equals("debug")) {
                        debugStats();
                    } else {
                        updatePeers(dataReceive);
                    }
                    // System.out.println("Got data: " + dataReceive);
                    // performUpdate();
                    // PeerHandler peer = handler.peerMap.get(1);
                    // System.out.println(handler.peerMap + " asking to update " + peer);
                    // peer.askToUpdate();
                    
                    receiveWriterSocket.close();
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

        // InetAddress localAddress = InetAddress.getLocalHost();
        // String serv = localAddress.toString().split("\\.")[0];
        // Integer sid = Integer.parseInt(serv.substring(2,4));
        // try {
        //     System.out.println("SLEEP " + sid);
        //     TimeUnit.SECONDS.sleep(sid);
        //     TimeUnit.SECONDS.sleep(sid);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }


        ConnectionHandler connections = new ConnectionHandler(socketMap);
        Thread connectionHandler = new Thread(connections);
        connectionHandler.start();
        

        connectionHandler.join();
        System.out.println("running main");
    }
    
}