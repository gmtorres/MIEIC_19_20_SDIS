
public class SendMessage implements Runnable{
	
	byte[] data;
	Channel channel;
	
	public SendMessage(byte[] msg, Channel chan) {
		data = msg;
		channel = chan;
	}

	@Override
	public void run() {
		channel.sendMessage(data);
	}

}
