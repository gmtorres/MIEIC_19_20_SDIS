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

		sender_id = Integer.parseInt(headerStr[2]);
		
		fileId = headerStr[3];

		chunkNo = Integer.parseInt(headerStr[4]);

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


        if(op.equals("CHUNK")) { 
			if (version == 1.0) {
				if (peer.getMemory().isPeerRequesting(fileId + "_" + String.valueOf(chunkNo)) == 1) {
					peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
					FileInfo fI = peer.getMemory().getRestoreFile(fileId); 
                    if(fI != null)
                    	fI.setFilePart(chunkNo, body);
				}
	
				else {
					//System.out.println(peer.getMemory().isPeerRequesting(fileId + "_" + String.valueOf(chunkNo)));
					peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
					//System.out.println("Chunk is not for me.");
				}
			}

			else if (version == 2.0) {
				if (peer.getMemory().isPeerRequesting(fileId + "_" + String.valueOf(chunkNo)) == 1) {
				}
				else {
					peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
				}
			}

        }
		
		
	}

}
