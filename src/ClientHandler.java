import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.*;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    Memory memory;
    String op;
	int sender_id;
    int chunkNo;
    byte[] header = null,body = null;
    InitPeer peer;
    String fileId;

    public ClientHandler(Socket socket, Memory mem, InitPeer peer) {
        this.clientSocket = socket;
        this.memory = mem;
        this.peer = peer;
    }

    private void getHeaderAndBody(byte[] buf) {
		for(int i = 0; i < buf.length - 4 ; ++i) {
			if(buf[i] == 0xD && buf[i+1] == 0xA && buf[i+2] == 0xD && buf[i+3] == 0xA) {
				header = Arrays.copyOf(buf, i);
				body = Arrays.copyOfRange(buf,i+4,buf.length);
			}
		}
	}

    public void run() {
        try {
        	
            InputStream in = clientSocket.getInputStream();
            DataInputStream dis = new DataInputStream(in);
            int len = dis.readInt();
            byte[] buf = new byte[len];
            if (len > 0) {
                dis.readFully(buf);
            }
            
            getHeaderAndBody(buf);
            
            String headerS = new String(header, StandardCharsets.UTF_8);
            String [] headerStr = headerS.split(" ");
            op = headerStr[1];
            sender_id = Integer.parseInt(headerStr[2]);
            fileId = headerStr[3];
            chunkNo = Integer.parseInt(headerStr[4]);
            
            if(op.equals("CHUNK")) { 
                if (peer.getMemory().isPeerRequesting(fileId + "_" + String.valueOf(chunkNo)) == 1) {
                    peer.getMemory().getRestoreFile().setFilePart(chunkNo, body);
                    peer.getMemory().eliminateRequestedChunk(fileId + "_" + String.valueOf(chunkNo));
                }
    
                else {
                    System.out.println("Chunk already restored"); //Meti mensagem diferente porque neste caso é sempre o peer a pedir, 
                                                                  //pode e ter ocorrido um processo ao mesmo tempo
                }
                
            }
            
            clientSocket.close();
    

    } catch (IOException e) {
        e.printStackTrace();
    }

    }
}