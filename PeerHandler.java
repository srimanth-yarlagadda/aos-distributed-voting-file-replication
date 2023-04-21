import java.net.*;

public class PeerHandler implements Runnable {

    Socket socket;

    public PeerHandler assignCommunicationSocket(Socket socket) {
        this.socket = socket;
        return this;
    }

    public void run() {
        System.out.println("Peer: " + socket.getInetAddress());
    }
    
}
