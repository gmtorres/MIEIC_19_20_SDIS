import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class ServerThread implements Runnable {

    private InitPeer p;
    ServerSocket serverSocket;

    ServerThread(InitPeer peer) {
        this.p = peer;
        try {
            this.serverSocket = new ServerSocket(6666 + p.getId(), 10000, InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (BindException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } 
        
    }

    public void close() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (!p.getMemory().getRestoreFile().checkFlag()) {
            try {
                p.getExecuter().execute(new ClientHandler(this.serverSocket.accept(), p.getMemory(), this.p));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    

}