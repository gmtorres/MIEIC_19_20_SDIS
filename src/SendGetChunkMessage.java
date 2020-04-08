import java.net.InetAddress;
import java.net.UnknownHostException;

public class SendGetChunkMessage implements Runnable {

    static final String CRLF = String.valueOf((char) 0xD) + String.valueOf((char) 0xA);

    InitPeer peer;
    byte[] message;

    SendGetChunkMessage(String fileId, int chunkNo, InitPeer p){
        String str = String.valueOf(p.getVersion()) + " GETCHUNK " + String.valueOf(p.getId()) + " " + fileId + " "
                + chunkNo + " " + CRLF + CRLF;

        if (p.getVersion() == 2.0) {
            try {
            InetAddress addressServer = InetAddress.getLocalHost();
            String addressString = addressServer.getHostAddress();
            int port_num = 6666 + p.getId();
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

        peer.getControlChannel().sendMessage(message);
    }

}