public class Server implements Runnable {

    public void run() {
        //
    }

    public static void main(String[] args) throws Exception {

        ConnectionHandler connections = new ConnectionHandler();
        Thread connectionHandler = new Thread(connections);
        connectionHandler.start();
        connectionHandler.join();
        
        System.out.println("running main");
    }
    
}