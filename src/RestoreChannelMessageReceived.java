import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RestoreChannelMessageReceived implements Runnable{
	
	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	double version;
	String op;
	int sender_id;
	String fileId;
	
	int chunkNo;
	
    byte[] data;
    
    byte[] header = null,body = null;
	
	InitPeer peer;
		
	
	RestoreChannelMessageReceived(byte[] buffer,InitPeer p){

        getHeaderAndBody(buffer);
        String headerS = new String(header, StandardCharsets.UTF_8);
        String [] headerStr = headerS.split(" ");
        

		version = Double.parseDouble(headerStr[0]);
		op = headerStr[1];
		//System.out.println("Operacao: " + op);

		//System.out.println("Sender_id111: " + headerStr[2]);
		sender_id = Integer.parseInt(headerStr[2]);
		//System.out.println("Sender_id: " + sender_id);
		
		fileId = headerStr[3];
		//System.out.println("fileId: " + fileId);

		chunkNo = Integer.parseInt(headerStr[4]);
		//System.out.println("chunkNo: " + chunkNo);

		peer = p;
    }
    
    private void getHeaderAndBody(byte[] buf) {
		for(int i = 0; i < buf.length - 4 ; ++i) {
			if(buf[i] == 0xD && buf[i+1] == 0xA && buf[i+2] == 0xD && buf[i+3] == 0xA) {
				header = Arrays.copyOf(buf, i);
				body = Arrays.copyOfRange(buf,i+4,buf.length);
			}
		}
	}


	@Override
	public void run() {


        if(op.equals("CHUNK")) { 
			if (version == 1.0) {
				if (peer.getMemory().isPeerRequesting(fileId + "_" + String.valueOf(chunkNo)) == 1) {
					peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
					peer.getMemory().getRestoreFile().setFilePart(chunkNo, body);
				}
	
				else {
					System.out.println(peer.getMemory().isPeerRequesting(fileId + "_" + String.valueOf(chunkNo)));
					peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
					System.out.println("Chunk is not for me.");
				}
			}

			else if (version == 2.0) {
				if (peer.getMemory().isPeerRequesting(fileId + "_" + String.valueOf(chunkNo)) == 1) {
					//peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
				}
				else {
					peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
				}
			}

        }
		
		
	}

}
