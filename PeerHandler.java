import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class PeerHandler implements Runnable {

    Socket socket;
    boolean systemDebug = false;
    
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
    }

    public PeerHandler() {}

    public void askToSync(String msg) {
        System.out.println("Sync replica");
        while (true) {
            try {
                TimeUnit.MICROSECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (writeInstance != null) {
                if (writeInstance.outStream != null) {
                    break;
                }
            }
        }
        try{
            writeInstance.outStream.writeUTF(msg);
        } catch (IOException ex) {ex.printStackTrace();}
    }

    public void askToUpdate(String msg) {
        try{
            writeInstance.outStream.writeUTF(msg);
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
        while (true) {
            try{
                String updateRequest = inStream.readUTF();
                System.out.println("Got info from another server: " + updateRequest + " and my current state is " + parentPeerHandler.fileServer.fileStatus);
                String[] details = updateRequest.split(" ");
                if (Integer.parseInt(details[0]) < 0) {
                    parentPeerHandler.fileServer.fileStatus.put("VN", Integer.parseInt(details[0])*-1);
                    parentPeerHandler.fileServer.fileStatus.put("RU", Integer.parseInt(details[1]));
                    parentPeerHandler.fileServer.fileStatus.put("DS", Integer.parseInt(details[2]));
                    String temp = "";
                    for (int i = 3; i < details.length; i++) {
                        temp += " " + details[i];
                    }
                    parentPeerHandler.fileServer.fileData = temp;
                } else {
                    parentPeerHandler.fileServer.fileStatus.put("VN", Integer.parseInt(details[0]));
                    parentPeerHandler.fileServer.fileStatus.put("RU", Integer.parseInt(details[1]));
                    parentPeerHandler.fileServer.fileStatus.put("DS", Integer.parseInt(details[2]));
                    if (parentPeerHandler.fileServer.fileData.equals("")) {
                        parentPeerHandler.fileServer.fileData = details[3];
                    } else {
                        parentPeerHandler.fileServer.fileData += " " + details[3];
                    }
                }
                System.out.println("Update complete: " + parentPeerHandler.fileServer.fileStatus + parentPeerHandler.fileServer.fileData);
                parentPeerHandler.fileServer.printFileStatus();
            } catch (IOException ex) {
                // ex.printStackTrace();
            }
        }
    }

    public void run() {
        System.out.println("Peer: " + socket.getInetAddress());
        String pidS = socket.getInetAddress().toString().split("\\.")[0].substring(2,4);
        Integer id = Integer.parseInt(pidS);
        final PeerHandler currentInstance  = this;

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
