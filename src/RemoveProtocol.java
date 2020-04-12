
public class RemoveProtocol implements Runnable{
	
	InitPeer peer;
	String file_id;
	int chunkNo;
	int desiredReplication;
	int perceivedReplication;
	
	public RemoveProtocol(InitPeer p,String fI,int n, int d, int per) {
		this.peer = p;
		this.file_id = fI;
		this.chunkNo = n;
		this.desiredReplication = d;
		this.perceivedReplication = per;
	}

	@Override
	public void run() {
		
		String key = file_id+"_"+chunkNo;
		if(peer.getMemory().isDeletedChunkToSend(key)) {
			peer.getMemory().removeDeletedChunk(key);
			byte [] data = peer.getMemory().getChunkData(key);
			if(data == null)
				return;
			//else System.out.println(data.length);
			//SEND PUTCHUNK AFTER A GIVEN TIME IF IT HASNT RECEIVED YET
			peer.getExecuter().execute(new SendPutChunkMessage(file_id , chunkNo , data , desiredReplication ,perceivedReplication , peer));
		}
		
	}

}
