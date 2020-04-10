import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BackupChannelMessageReceived implements Runnable{
	
	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	double version;
	String op;
	int sender_id;
	String fileId;
	
	int chunkNo;
	int replicationDegree;
	
	byte[] header = null,body = null;
	
	InitPeer peer;
		
	
	BackupChannelMessageReceived(byte[] buffer,InitPeer p){
		getHeaderAndBody(buffer);
		
		String headerS = new String(header, StandardCharsets.UTF_8);
		String [] headerStr = headerS.split(" ");
		
		
		version = Double.parseDouble(headerStr[0]);
		op = headerStr[1];
		sender_id = Integer.parseInt(headerStr[2]);
		fileId = headerStr[3];
		chunkNo = Integer.parseInt(headerStr[4]);
		replicationDegree = Integer.parseInt(headerStr[5]);
		peer = p;
	}
	
	private void getHeaderAndBody(byte[] buf) {
		for(int i = 0; i < buf.length - 4 ; ++i) {
			if(buf[i] == 0xD && buf[i+1] == 0xA && buf[i+2] == 0xD && buf[i+3] == 0xA) {
				header = Arrays.copyOf(buf, i);
				body = Arrays.copyOfRange(buf,i+4,buf.length);
				break;
			}
		}
	}

	@Override
	public void run() {
		
		
		if(op.equals("PUTCHUNK") && sender_id != this.peer.getId()) {
			
			if(peer.getMemory().getFileDataByID(fileId) != null) {
				System.out.println("Received chunk of file whose backup was initiated by this peer.");
				return;
			}
			
			if(peer.getMemory().isDeletedChunkToSend(fileId + "_" + chunkNo)) {
				peer.getMemory().removeDeletedChunk(fileId + "_" + chunkNo);
				System.out.println("Received chunk of file that was going to be transmited");
			}
			else if(!peer.getMemory().isChunkStored(fileId + "_" + chunkNo)) { //se nao estiver guardado
				
				String confirmation = String.valueOf(peer.getVersion()) + " STORED " + String.valueOf(peer.getId()) + " " + fileId + " " + chunkNo +" "+ CRLF ;
				
				if(peer.getMemory().storeChunk(fileId + "_" + chunkNo, body, replicationDegree)) {
					System.out.println("chunk " + fileId + "_" + chunkNo + " stored");
					int delay = ThreadLocalRandom.current().nextInt(0, 400 + 1);
					if(peer.getVersion() == 1)
						peer.getExecuter().schedule(new SendMessage(confirmation.getBytes(),peer.getControlChannel()),delay,TimeUnit.MILLISECONDS);
					else if(peer.getVersion() == 2)
						peer.getExecuter().schedule(new SendStoredChunkMessage_v2(fileId,chunkNo,this.peer), delay, TimeUnit.MILLISECONDS);
				}else {
					System.out.println("No space to store chunk");
				}
				
			}else {
				System.out.println("Already stored");
			}

			
		}
		
	}

}
