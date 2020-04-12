import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class ServerThread implements Runnable {

    private InitPeer p;
    private ServerSocket serverSocket;
    private int port = 1025;

    ServerThread(InitPeer peer) {
        this.p = peer;
        int offset = 0;
        while(true) {
        	try {
            	port = 1025 + ( peer.getId() +  offset) % (65536- 1025);
                this.serverSocket = new ServerSocket(port, 10000, InetAddress.getLocalHost());
                break;
            } catch (UnknownHostException e) {
                e.printStackTrace();
                break;
            } catch (BindException e) {
            	System.out.println(e);
            	if(offset < (65536- 1025)) {
            		System.out.println("Trying another port");
            		offset++;
            	}else {
            		System.out.println("Could find valid port for socket");
            		break;
            	}
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        
    }
    
    public int getPort() {
    	return port;
    }

    public void close() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                p.getExecuter().execute(new ClientHandler(this.serverSocket.accept(), p.getMemory(), this.p));
            } catch (IOException e) {
            	if(this.serverSocket.isClosed())
            		System.out.println("Server closed");
            	else
            		System.out.println(e.toString());
                break;
            }
        }
    }

    

}