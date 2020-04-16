
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
	
	public static void main(String[] args) {

		
		if(args.length < 2) {
			System.out.println("Wrong usage at least 2 arguments: App ip:acess_point");
			System.exit(-1);
		}
		
		String[] peerArgs = args[0].split(":");
		String host = null;
		String access_point = null;
		if(peerArgs.length > 2) {
			System.out.println("Wrong usage of ip/acess_point");
			System.exit(-1);
		}else if(peerArgs.length == 1) {
			host = "localhost";
			access_point = peerArgs[0];
		}else {
			host = peerArgs[0];
			access_point = peerArgs[1];
		}
		
		String op = args[1];
		
		String file_name;
		
		try {
			Registry registry = LocateRegistry.getRegistry(host);
			RemoteInterface peer = (RemoteInterface) registry.lookup(access_point);
			System.out.println("App ready");
			
			switch(op) {
			case "BACKUP":
				if(args.length != 4) {
					System.out.println("Wrong usage: App ip/access_point BACKUP file_path replication_degree");
					System.exit(-1);
				}
				file_name = args[2];
				int replication = Integer.parseInt(args[3]);
				System.out.println(peer.backup(file_name,replication));
				break;
			case "RESTORE":
				if(args.length != 3) {
					System.out.println("Wrong usage: App ip/access_point RESTORE file_path");
					System.exit(-1);
				}
				file_name = args[2];
				System.out.println(peer.restore(file_name));
				break;
			case "RECLAIM":
				int newMemory = Integer.parseInt(args[2]);
				System.out.println(peer.manage(newMemory));
				break;
			case "DELETE":
				if(args.length != 3) {
					System.out.println("Wrong usage: App ip/access_point DELETE file_path");
					System.exit(-1);
				}
				file_name = args[2];
				System.out.println(peer.delete(file_name));
				break;
			case "STATUS":
				System.out.println(peer.state());
				break;
			default:
				System.out.println("Operation not recognized");
			}
			
		}catch(Exception e) {
			System.out.print(e);
			return;
		}
		
	}

}
