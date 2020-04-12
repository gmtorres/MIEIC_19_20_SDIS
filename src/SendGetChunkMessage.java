import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class SendGetChunkMessage implements Runnable {

    static final String CRLF = String.valueOf((char) 0xD) + String.valueOf((char) 0xA);

    InitPeer peer;
    byte[] message;
    boolean checking = false;
    String fileId;
    int chunkNo;

    SendGetChunkMessage(String fileId, int chunkNo, InitPeer p){
    	this.fileId = fileId;
    	this.chunkNo = chunkNo;
        String str = String.valueOf(p.getVersion()) + " GETCHUNK " + String.valueOf(p.getId()) + " " + fileId + " "
                + chunkNo + " " + CRLF + CRLF;

        if (p.getVersion() == 2.0) {
            try {
            InetAddress addressServer = InetAddress.getLocalHost();
            String addressString = addressServer.getHostAddress();
            int port_num = p.getServerThread().getPort();
            addressString += "/";
            addressString += String.valueOf(port_num);

            str += addressString;
            }
            catch(UnknownHostException e) {
                e.printStackTrace();
            }
        }
        
        message = str.getBytes();
        peer = p;
    } 

    @Override
    public void run() {
    	if(checking == false) {
    		peer.getControlChannel().sendMessage(message);
    		this.checking = true;
    		this.peer.getExecuter().schedule(this, 800, TimeUnit.MILLISECONDS);
    	}else {
    		if(this.peer.getMemory().getRestoreFile(this.fileId).getFilePart(this.chunkNo) == null) {
    			this.peer.getMemory().getRestoreFile(this.fileId).restoreFailed();
    		}
    	}
        
        
        
    }

}