import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This thread will listen for new connection requests
 */
public class ConnectionsListenerThread implements Runnable{

    private ServerSocket serverSocket;
    private int noOfConnections;

    public ConnectionsListenerThread(ServerSocket serverSocket, int noOfConnections){
        this.serverSocket = serverSocket;
        this.noOfConnections = noOfConnections;
    }

    @Override
    public void run() {
        while(noOfConnections-- > 0){
            try {
                Socket newConnection = serverSocket.accept();
                Thread thread = new Thread(new PeerMessageListenerThread(newConnection, true));
                PeerApplication.addListenerThread(thread);
                thread.start();

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
