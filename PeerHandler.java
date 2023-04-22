import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class PeerHandler implements Runnable {

    Socket socket;
    boolean systemDebug = false;

    public PeerHandler assignCommunicationSocket(Socket socket) {
        this.socket = socket;
        return this;
    }

    public void run() {
        System.out.println("Peer: " + socket.getInetAddress());
        String pidS = socket.getInetAddress().toString().split("\\.")[0].substring(2,4);
        Integer id = Integer.parseInt(pidS);

        // Testing - remove later
        // try {
        //     if (id == 2) {
        //         DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        //         dos.writeUTF("hello");
        //         try {
        //             TimeUnit.SECONDS.sleep(5);
        //             System.out.println("....5....");
        //             TimeUnit.SECONDS.sleep(8);
        //         } catch (InterruptedException e) {e.printStackTrace();}
        //         dos.writeUTF("world");
        //         System.out.println("....wrote....");
        //     }

        //     if (id == 3) {
        //         DataInputStream dos = new DataInputStream(socket.getInputStream());
        //         String s = dos.readUTF();
        //         System.out.println("One >> " + s);
        //         s = dos.readUTF();
        //         System.out.println("Two >> " + s);
        //     }
        // } catch (IOException e) {
        //     if (systemDebug) {e.printStackTrace();}
        // }
    }
    
}
