import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class SendChunkMessage implements Runnable{
	
	InitPeer peer;
	String file_id;
	int chunkNo;
	double version;
	byte[] message;
	byte[] messagev2;
	InetAddress connectionAdd;
	int port_num;
	double messageVersion;
	
	public SendChunkMessage(String fI, int c_no, double v, InitPeer p, byte[] m, byte[] m2, InetAddress add, int prt, double message_v) {
		this.file_id = fI;
		this.chunkNo = c_no;
		this.version = v;
		this.peer = p;
		this.message = m;
		this.messagev2 = m2;
		this.connectionAdd = add;
		this.port_num = prt; 
		this.messageVersion = message_v;
	}

	@Override
	public void run() {
		if(peer.getMemory().getRequestedChunk(file_id + "_" + String.valueOf(chunkNo)) != -1) {
			if(version == 1.0) {
				peer.getMemory().eliminateRequestedChunk(file_id + "_" + String.valueOf(chunkNo));
				peer.getRestoreChannel().sendMessage(message);	
			}else if(version == 2.0) {
				if (messageVersion == 1.0) {
					peer.getMemory().eliminateRequestedChunk(file_id + "_" + String.valueOf(chunkNo));
					peer.getRestoreChannel().sendMessage(message);
				}else {
					peer.getMemory().eliminateRequestedChunk(file_id + "_" + String.valueOf(chunkNo));
					peer.getRestoreChannel().sendMessage(messagev2);

					try {
						Socket clientSocket = new Socket(connectionAdd, port_num);
						OutputStream out = clientSocket.getOutputStream(); 
					    DataOutputStream dos = new DataOutputStream(out);
					    dos.writeInt(message.length);
					    dos.write(message,0,message.length);
						clientSocket.close();
					}catch (IOException e) {
						e.printStackTrace();
					}
					
				}
			}
			System.out.println("Sent chunk " + file_id + "_" + String.valueOf(chunkNo));
		}else 
			return;
		
	}

}
