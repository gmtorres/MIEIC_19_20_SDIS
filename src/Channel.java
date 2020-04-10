import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel {
	
	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	InetAddress group;
	MulticastSocket s;
	int port;
	
	Channel(String address, int p)  {
		port = p;
		try {
			group = InetAddress.getByName(address);
			s = new MulticastSocket(port);
		}catch(Exception e) {
			System.out.print(e);
			System.exit(-2);
		}
	}
	
	public void sendMessage(byte[] data) {
		try {
			DatagramSocket sender_socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(data, data.length,group,port);
			sender_socket.send(packet);
			sender_socket.close();
		}catch (IOException ex) {
            ex.printStackTrace();
        }
	}
}
