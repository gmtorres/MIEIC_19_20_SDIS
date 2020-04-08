
public class SendStoredChunkMessage_v2 implements Runnable {
	
	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;

	InitPeer peer;
	String fileId;
	int chunkNo;
	byte [] msg;
	
	public SendStoredChunkMessage_v2(String fId, int cNo, InitPeer p) {
		
		peer = p;
		fileId = fId;
		chunkNo = cNo;
		
		String confirmation = String.valueOf(peer.getVersion()) + " STORED " + String.valueOf(peer.getId()) + " " + fileId + " " + chunkNo +" "+ CRLF ;
		msg = confirmation.getBytes();
		
	}
	
	public void run() {
		int perceived = peer.getMemory().getStoredChunkPerceivedReplication(fileId + "_" + chunkNo);
		int desired = peer.getMemory().getStoredChunkDesiredReplication(fileId + "_" + chunkNo);
		//System.out.println(perceived + "   " + desired);
		if(perceived > desired) {
			peer.getMemory().removeStoredChunk(fileId + "_" + chunkNo);
		}else {
			peer.getExecuter().execute(new SendMessage(msg,peer.getControlChannel()));
		}
		
	}
	
}
