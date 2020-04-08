import java.util.ArrayList;

import javafx.util.Pair;

public class SendFilesToDeleteMessage implements Runnable{
	
	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	InitPeer peer;
	
	public SendFilesToDeleteMessage(InitPeer p) {
		peer = p;
	}

	@Override
	public void run() {
		String str;
		ArrayList<Pair<String,Integer>> l = this.peer.getMemory().getFileDeletedList();
		for(Pair<String,Integer> p : l) {
			str = this.peer.getVersion() + " DELETE " + this.peer.getId() + " " + p.getKey() + " " + p.getValue() + " " + CRLF ;
			this.peer.getExecuter().execute(new SendMessage(str.getBytes(),this.peer.getControlChannel()));
		}
	}
	
	
	
}
