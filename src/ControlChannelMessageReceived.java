import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ControlChannelMessageReceived implements Runnable {

	static final String CRLF = String.valueOf((char) 0xD) + String.valueOf((char) 0xA);

	double version;
	String op;
	int sender_id;
	String file_id;

	int chunkNo;

	byte[] header = null, body = null;

	InitPeer peer;

	InetAddress connectionAdd;
	int port_num;

	ControlChannelMessageReceived(byte[] message, InitPeer p) {
		String str = new String(message, StandardCharsets.UTF_8);

		String[] headerStr = str.split(" ");

		version = Double.parseDouble(headerStr[0]);
		op = headerStr[1];

		if (version == 2.0 && op.equals("GETCHUNK")) {
			getHeaderAndBody(message);
			try {
				String connectionInfo = new String(body);
				String[] connectionData = connectionInfo.split("/");
				 connectionAdd = InetAddress.getByName(connectionData[0]);
				 port_num = Integer.valueOf(connectionData[1]);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}

		sender_id = Integer.parseInt(headerStr[2]);
		if(!op.equals("GETDELETED"))
			file_id = headerStr[3];
		peer = p;
		
		if(op.equals("STORED") || op.equals("GETCHUNK") || op.equals("REMOVED")) {
			chunkNo = Integer.parseInt(headerStr[4]);
		}
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

		if(peer.getId() == sender_id)
			return;
		
		if(op.equals("STORED")) {
			
			System.out.println("Received STORED of chunk : " + file_id + "_" + String.valueOf(chunkNo));
			// increment chunk replication degree of chunk sent, to know how many peers have the chunk
			if(peer.getMemory().incrementChunkReplication(file_id + "_" + String.valueOf(chunkNo)) == -1) { //se n�o estiver guardado neste computador
				if(peer.getMemory().isChunkStored(file_id + "_" + String.valueOf(chunkNo))) { // ver se o chunk est� stored e incrementar
					peer.getMemory().incrementChunkPerceivedReplication(file_id + "_" + String.valueOf(chunkNo));
				}
				
			}
			
		}

		if(op.equals("GETCHUNK")) {

			if(((peer.getMemory().isChunkStored(file_id + "_" + String.valueOf(chunkNo))) == false)) {
				return;
			}
			else {
				peer.getMemory().addRequestedChunk(file_id + "_" + String.valueOf(chunkNo));
			}
			System.out.println("Received GETCHUNK");

			double messageVersion = Math.min(version, peer.getVersion());
			
			String header = String.valueOf(messageVersion) + " CHUNK " + peer.getId() + " " + file_id + " " + String.valueOf(chunkNo) + " " + CRLF + CRLF;
			byte [] headerB = header.getBytes();
			byte[] fileData = peer.getMemory().getChunkData(file_id + "_" + String.valueOf(chunkNo));
			
			byte[] nullBody = new String(" ").getBytes();
			
			byte [] message = new byte[headerB.length + fileData.length];
			byte [] messagev2 = new byte[headerB.length + nullBody.length];

			System.arraycopy(headerB, 0, messagev2, 0, headerB.length);
			System.arraycopy(nullBody, 0, messagev2, headerB.length, nullBody.length);

			System.arraycopy(headerB, 0, message, 0, headerB.length);
			System.arraycopy(fileData, 0, message, headerB.length, fileData.length);

			int ms = new Random().nextInt(401);
			this.peer.getExecuter().schedule(new SendChunkMessage(file_id, chunkNo, version, this.peer, message, messagev2, connectionAdd, port_num, messageVersion) , ms, TimeUnit.MILLISECONDS);

		}else if(op.equals("DELETE")) {
			System.out.println("let's delete");
			if(peer.getMemory().removeFile(file_id)) {
				System.out.println("File " + file_id + " removed");
				if(peer.getVersion() == 2.0) {
					String message = this.peer.getVersion() + " DELETED " + this.peer.getId() + " " + file_id + " " + CRLF + CRLF;
					int delay = ThreadLocalRandom.current().nextInt(0, 400 + 1);
					this.peer.getExecuter().schedule(new SendMessage(message.getBytes(),peer.getControlChannel()),delay,TimeUnit.MILLISECONDS);
				}
			}else {
				System.out.println("This peer had no chunks stored of that file ");
			}
			
		}
		else if(op.equals("REMOVED")) {
			
			String key = file_id+"_"+chunkNo;
			
			System.out.println("FILE REMOVED RECEIVED");
			
			peer.getMemory().decrementChunkReplication(key); //in case the file has been backup by this peer decrement the perceived replication
			
			int perceivedReplication = peer.getMemory().getStoredChunkPerceivedReplication(key);
			if(perceivedReplication == -1) //chunk does not exist in memory
				return;
			
			peer.getMemory().decrementChunkPerceivedReplication(key);
			perceivedReplication--;
			
			int desiredReplication = peer.getMemory().getStoredChunkDesiredReplication(key);
			
			if(perceivedReplication < desiredReplication ) { // perceivedRep is less than the desired
				
				System.out.println("MUST INITIATE PROTOCOL");
				
				peer.getMemory().addDeletedChunk(key);
			
				try {
					Thread.sleep(new Random().nextInt(401));
				}
				catch(Exception e) {
					System.out.println(e);
				}
				
				if(peer.getMemory().isDeletedChunkToSend(key)) {
					peer.getMemory().removeDeletedChunk(key);
					byte [] data = peer.getMemory().getChunkData(key);
					if(data == null)
						return;
					else System.out.println(data.length);
					//SEND PUTCHUNK AFTER A GIVEN TIME IF IT HASNT RECEIVED YET
					peer.getExecuter().execute(new SendPutChunkMessage(file_id , chunkNo , data , desiredReplication , perceivedReplication, peer));
				}
				
			}
			
		}else if(op.equals("DELETED")) {
			if(this.peer.getMemory().isFileInFileDeleted(file_id)) {
				if(this.peer.getMemory().decrementFileDeletePerceived(file_id)) {
					System.out.println("File deleted from all peers");
				}
			}
		}else if(op.equals("GETDELETED")) {
			if(this.peer.getVersion() == 2) {
				int delay = ThreadLocalRandom.current().nextInt(0, 400 + 1);
				this.peer.getExecuter().schedule(new SendFilesToDeleteMessage(peer) ,delay,TimeUnit.MILLISECONDS);
			}
		}
		
		
	}

}
