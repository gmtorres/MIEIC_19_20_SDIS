import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

public class RestoreChannel extends Channel implements Runnable {
	
	InitPeer peer;
	
	RestoreChannel(String address, int port, InitPeer p) {
		super(address,port);
		peer = p;
	}

	@Override
	public void run() {

		byte[] buffer = new byte[65000];

		try {
			s.joinGroup(group);

			while(true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				s.receive(packet);
				byte[] buf = Arrays.copyOf(buffer, packet.getLength());


				peer.getExecuter().execute(new RestoreChannelMessageReceived(buf,peer));
				buffer = new byte[65000];
				
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		s.close();
	}

}
