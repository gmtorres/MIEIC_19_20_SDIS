
public class SendGetFilesToDeleteMessage implements Runnable{
	
	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	InitPeer peer;
	
	public SendGetFilesToDeleteMessage(InitPeer p) {
		this.peer = p;
	}
	
	@Override
	public void run() {
		String message = this.peer.getVersion() + " GETDELETED " + this.peer.getId() + " " + CRLF + " " + CRLF + " " + CRLF ;
		peer.getExecuter().execute(new SendMessage(message.getBytes(),this.peer.getControlChannel()));
	}
	
}
