import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class PeerHandler implements Runnable {

    Socket socket;
    boolean systemDebug = false;
    public static Integer peerID;
    public Integer SendUpdates;
    
    Thread writeThread, readThread;
    PeerHandler writeInstance, readInstance;
    public DataOutputStream outStream;
    public DataInputStream inStream;

    PeerHandler parentPeerHandler;
    Server fileServer;

    public PeerHandler(Server fileServer) {
        this.fileServer = fileServer;
    }

    public PeerHandler assignCommunicationSocket(Socket socket) {
        this.socket = socket;
        return this;
    }

    public PeerHandler(PeerHandler ph, Socket s) {
        this.socket = s;
        this.parentPeerHandler = ph;
        this.SendUpdates = 0;
    }

    public PeerHandler() {}

    public void setSendUpdates(Integer ct) {
        SendUpdates = ct;
        // System.out.println("writes updated " + ct);
    }

    public void askToUpdate(String msg) {
        Integer currentVersion = fileServer.fileStatus.get("VN");
        try{
            writeInstance.outStream.writeInt(currentVersion);
            // Integer statusResponse = readInstance.inStream.readInt();
            // System.out.println("wait for response");
            Integer statusResponse = 0;
            while (true) {
                try {
                    TimeUnit.MICROSECONDS.sleep(1);
                    if (SendUpdates != 0) {break;}
                } catch (InterruptedException e) {}
            }
            statusResponse = SendUpdates;
            setSendUpdates(0); // = 0;
            // System.out.println("got response " + statusResponse);
            // System.out.println("writer c/sr:" + currentVersion + " " + statusResponse);
            // if (statusResponse == -1) {
            //     writeInstance.outStream.writeUTF(msg);
            // } else if (currentVersion > statusResponse) {
            for (int version = currentVersion-statusResponse+1; version <= currentVersion; version++) {
                try{
                    // System.out.println("\t\tSyncing: " + (version) + " with " + peerID);
                    writeInstance.outStream.writeUTF(version + " " + fileServer.fileStatus.get("RU") + " " + fileServer.fileStatus.get("DS") + " " + fileServer.fileUpdateLog.get(version));
                } catch (IOException ex) {ex.printStackTrace();}
            }
            // } else {
            //     System.out.println("unhandled");
            // }
            
        } catch (IOException ex) {ex.printStackTrace();}
    }

    public void writer() {
        try{
            outStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            // ex.printStackTrace();
        }
    }

    public void reader() {
        try{inStream = new DataInputStream(socket.getInputStream());} catch (IOException ex) {}
        while (true && !socket.isClosed()) {
            try{
                // System.out.println("read start");
                Integer nextVersion = inStream.readInt();
                // System.out.println("read done: " + nextVersion);
                if (nextVersion < 0) {
                    // System.out.println("Updating writes to write " + parentPeerHandler.SendUpdates);
                    parentPeerHandler.setSendUpdates(-1*nextVersion);
                } else {
                    Integer currentVersion = parentPeerHandler.fileServer.fileStatus.get("VN");
                    // System.out.println("reader c/n:" + currentVersion + " " + nextVersion);
                    Integer reads = nextVersion - currentVersion;
                    // if (reads == 1) {
                    //     parentPeerHandler.writeInstance.outStream.writeInt(-1);
                    // } else {
                    parentPeerHandler.writeInstance.outStream.writeInt(-1*reads);
                    // }
                    // System.out.println("Done: current" + currentVersion + " next:" + nextVersion + " reads: " + reads);
                    while (reads != 0) {
                        String updateRequest = inStream.readUTF();
                        if (systemDebug) {System.out.println("Got info from another server: " + updateRequest + " and my current state is " + parentPeerHandler.fileServer.fileStatus);}
                        String[] details = updateRequest.split(" ");
                        parentPeerHandler.fileServer.fileStatus.put("VN", Integer.parseInt(details[0]));
                        parentPeerHandler.fileServer.fileStatus.put("RU", Integer.parseInt(details[1]));
                        parentPeerHandler.fileServer.fileStatus.put("DS", Integer.parseInt(details[2]));
                        if (parentPeerHandler.fileServer.fileData.equals("")) {
                            parentPeerHandler.fileServer.fileData = details[3];
                        } else {
                            parentPeerHandler.fileServer.fileData += " " + details[3];
                        }
                        System.out.println("Update : \033[1m\033[32m" + parentPeerHandler.fileServer.fileStatus + " : " + details[3] + "\033[0m");
                        parentPeerHandler.fileServer.printFileStatus();
                        reads--;
                    }
                }
            } catch (IOException ex) {
                // ex.printStackTrace();
            }
        }
    }

    public void run() {
        System.out.println("Peer: " + socket.getInetAddress());
        String pidS = socket.getInetAddress().toString().split("\\.")[0].substring(2,4);
        peerID = Integer.parseInt(pidS);
        final PeerHandler currentInstance  = this;

        setSendUpdates(0);

        writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeInstance = new PeerHandler(currentInstance, socket);
                writeInstance.writer();
            }
        });
        writeThread.start();

        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new PeerHandler(currentInstance, socket).reader();
            }
        });
        readThread.start();
    


        while (socket != null) {
            try {
                 TimeUnit.MICROSECONDS.sleep(1);
            } catch (InterruptedException exc) {
                exc.printStackTrace();
            }
        }

    }
    
}
