import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;


public class SendPutChunkMessage implements Runnable{
	
	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	InitPeer peer;
	byte [] message;
	int max_tries = 5; 
	String key;
	int tries = 1;
	int replication;
	int time = 1;
	FileInfo fI = null;
	
	SendPutChunkMessage(String fileId , int chunkNo, byte[] data, int rep, int current_replication, InitPeer p){
		String header = String.valueOf(p.getVersion()) + " PUTCHUNK " + String.valueOf(p.getId()) + " " + fileId + " " + chunkNo + " " + rep + " " + CRLF + CRLF;
		byte[] headerB = header.getBytes();
		
		message = new byte[headerB.length + data.length];
		System.arraycopy(headerB, 0, message, 0, headerB.length);
		System.arraycopy(data, 0, message, headerB.length, data.length);
				
		peer = p;
		replication = rep;
		key = fileId + "_"+ String.valueOf(chunkNo);
		peer.getMemory().addChunckReplication(key,current_replication);
		
	}	
	
	SendPutChunkMessage(FileInfo file , int chunkNo, byte[] data, int rep, int current_replication, InitPeer p){
		this(file.getFileId(),chunkNo,data,rep,current_replication,p);
		fI = file;
	}	

	@Override
	public void run() {
		
		int rep = peer.getMemory().getChunkReplication(key);
		
		if(rep < replication) {
			peer.getBackupChannel().sendMessage(message);
			System.out.println("Sent try " + tries + " of chunk " + key);
			time*=2;
			tries++;
			
			if(tries <= max_tries)
				peer.getExecuter().schedule(this, time, TimeUnit.SECONDS);
			else if(fI != null){
				this.fI.incrementChunksBackedup();
				rep = peer.getMemory().getChunkReplication(key);
				if(rep == 0) {
					this.fI.setFileState(2);
				}else if(rep < replication) {
					this.fI.setFileState(1);
				}
			}
			
		}else if(fI != null){
			this.fI.incrementChunksBackedup();
		}
		
		
	}
}