import java.io.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class InitPeer implements RemoteInterface{	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	private ControlChannel MC;
    private BackupChannel MDB;
    private RestoreChannel MDR;
    
    private int peer_id;
    private double version;
    
	private Memory memory;
	
	private ServerThread serverThread;
    
    private ScheduledThreadPoolExecutor scheduler_executer;
	
	private InitPeer(double v, int p_id,String mcAddr, int mcPort, String mdbAddr, int mdbPort, String mdrAddr, int mdrPort) {
		if(v != 1 && v != 2) {
			System.out.println("Version must be 1 or 2");
			System.exit(-1);
		}
		if(p_id < 0) {
			System.out.println("Peer id must be greater than 0");
			System.exit(-1);
		}
		version = v;
		peer_id = p_id;
		MC = new ControlChannel(mcAddr,mcPort,this);
		MDB = new BackupChannel(mdbAddr,mdbPort,this);
		MDR = new RestoreChannel(mdrAddr,mdrPort,this);

		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
                "./peer" + this.getId() + "/memory" + this.getId() +  ".ser" ));

			memory = (Memory) ois.readObject();
		
			ois.close();
			System.out.println("Read memory from file");

		} catch(FileNotFoundException e) {
			memory = new Memory("./peer" + this.getId() + "/");
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		
		scheduler_executer = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(64);

		if (version == 2.0) {
			serverThread = new ServerThread(this);
			scheduler_executer.execute(this.serverThread);
		}

		
	}

    public String backup(String file_name, int replication) {
    	
        System.out.println("backup of file " + file_name);
        
        if(replication <= 0 || replication > 9) {
        	return "Replication degree must be between 1 and 9";
        }
        
        FileData fd = this.getMemory().getFileData(file_name);
		if(fd != null) {
			System.out.println("Found new version of file, deleting the old one");
			String message = this.getVersion() + " DELETE " + this.getId() + " " + fd.getFileId() + " " + CRLF + CRLF;
			this.getControlChannel().sendMessage(message.getBytes());
			this.getMemory().removeFile(fd.getFileId());
		}
		

        
        
        FileInfo file = new FileInfo(file_name, replication);
        this.memory.addBackupFile(file.getFileData());
        try {
        	for(int i = 0; i < file.getNumberOfParts(); i++) {
            	this.getExecuter().execute(new SendPutChunkMessage(file , i , file.getFilePart(i) , replication , 0, this));
            	Thread.sleep(20);
            } 
        }catch(Exception e) {
        	System.out.println(e);
        }     
        
        while(file.getChunksBackedup() != file.getNumberOfParts()) {}
        int state = file.getState();
        
        if(state == 1) {
        	return "File " + file_name + " backedup but could not achived desired replication degree in some chunks.";
        }else if(state == 2) {
        	return "Couldn't backup some chunks to any peer";
        }
        return "File " + file_name + " backedup successfully";
        
    }

    public String restore(String file_name) {

    	System.out.println("restore of file " + file_name);
		
		FileData fd = this.getMemory().getFileData(file_name);
		if(fd == null) {
			System.out.println("File not recognized");
			return "File has not been recognized by this peer";
		}


		FileInfo file = new FileInfo(fd);
		this.getMemory().changeRestoreFile(file);

		for(int i = 0; i < fd.getNumberOfChunks() ; i++) {
			this.getMemory().addRequestedChunkPeer(this.getMemory().getRestoreFile().getFileData().getFileId() + "_" + i);
		}

		for(int i = 0; i < fd.getNumberOfChunks() ; i++) {
			this.getExecuter().execute(new SendGetChunkMessage(this.getMemory().getRestoreFile().getFileData().getFileId() , i , this));
			try{
				Thread.sleep(20);
			}catch(Exception e) {
	        	System.out.println(e);
	        }  
		}
		

		while(!this.getMemory().getRestoreFile().checkFlag()) {}
		FileInfo f = this.getMemory().getRestoreFile();
		f.setFile(f.createFile(f.getFileData().getFilepath(), this.getMemory().path));

        return "File " + file_name + " restored successfully";
	}
	
    public String delete(String file_name) {
        System.out.println("delete");
        
        FileData fd = this.getMemory().getFileData(file_name);
		if(fd == null) {
			System.out.println("File not recognized");
			return "File has not been recognized by this peer";
		}
		
		String message = this.getVersion() + " DELETE " + this.getId() + " " + fd.getFileId() + " " ;
		if(this.getVersion() == 1) { 
			message+= CRLF + CRLF;
		}
		else if(this.getVersion() == 2.0) {
			message += fd.getPerceivedReplicationDegree() + " " + CRLF;
			this.getMemory().addFileDeleted(fd.getFileId(), fd.getPerceivedReplicationDegree());
		}
		this.getControlChannel().sendMessage(message.getBytes());
		this.getMemory().removeFile(fd.getFileId());
        
        return "Sended system delete of file " + file_name + " successfully";
    }
    public String manage(int newMemory) {
        System.out.println("manage");
        
        if(newMemory < 0) {
        	return "New memory size must be greater than 0";
        }
        
        this.getMemory().reclaimMemory(newMemory,this);
        return "Disk space managed successfully";
    }
    public String state() {
        System.out.println("state");
        
        return "-----  PEER " + this.getId() + "  -----\n" +  this.getMemory().getBackupedFile();
    }
    
    public ControlChannel getControlChannel() {
    	return MC;
    }
    public BackupChannel getBackupChannel() {
    	return MDB;
    }
    public RestoreChannel getRestoreChannel() {
    	return MDR;
    }
    
    public double getVersion() {
    	return version;
    }
    public int getId() {
    	return peer_id;
    }
    public ScheduledThreadPoolExecutor getExecuter() {
    	return scheduler_executer;
    }
    
    public Memory getMemory() {
    	return memory;
	}
	
	public ServerThread getServerThread() {
		return serverThread;
	}
	
	public static void main(String[] args)  throws IOException{
		
		if(args.length != 6) {
			System.out.println("Usage: Peer version server_id access_point MC_IP_address:MC_port MDB_IP_address:MDB_port MDR_IP_address:MDR_port");	
			System.exit(-1);
		}
		String [] ip_port = null;
		double version = Double.parseDouble(args[0]);
		int peer_id = Integer.parseInt(args[1]);
		String access_point = args[2];
		
		ip_port = args[3].split(":");
		String mcAddr="localhost";
		int mcPort;
		if(ip_port[0].equals("")) {
			mcPort = Integer.parseInt(ip_port[1]);
		}else if(ip_port.length == 1){
			mcPort = Integer.parseInt(ip_port[0]);
		}else {
			mcAddr = ip_port[0];
			mcPort = Integer.parseInt(ip_port[1]);
		}
		
		ip_port = args[4].split(":");
		String mdbAddr="localhost";
		int mdbPort;
		if(ip_port[0].equals("")) {
			mdbPort = Integer.parseInt(ip_port[0]);
		}else if(ip_port.length == 1){
			mdbPort = Integer.parseInt(ip_port[0]);
		}else {
			mdbAddr = ip_port[0];
			mdbPort = Integer.parseInt(ip_port[1]);
		}
		
		ip_port = args[5].split(":");
		String mdrAddr="localhost";
		int mdrPort;
		if(ip_port[0].equals("")) {
			mdrPort = Integer.parseInt(ip_port[0]);
		}else if(ip_port.length == 1){
			mdrPort = Integer.parseInt(ip_port[0]);
		}else {
			mdrAddr = ip_port[0];
			mdrPort = Integer.parseInt(ip_port[1]);
		}

		InitPeer peer = new InitPeer(version,peer_id,mcAddr,mcPort,mdbAddr,mdbPort,mdrAddr,mdrPort);
		try {
        	RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(peer, 0);

	        // Bind the remote object's stub in the registry
	        Registry registry = LocateRegistry.getRegistry();
	        registry.rebind(access_point, stub);
            System.err.println("Peer ready");

			
		} catch (Exception e1) {
	        System.err.println("Peer exception: " + e1.toString());
	        e1.printStackTrace();
	        System.exit(-1);
	    }
		
		File directory = new File("./peer" + peer.getId());
		if (!directory.exists()){
	        directory.mkdir();
	    }
		
		peer.scheduler_executer.execute(peer.getControlChannel());
		peer.scheduler_executer.execute(peer.getBackupChannel());
		peer.scheduler_executer.execute(peer.getRestoreChannel());
		
		if(peer.getVersion() == 2) {
			peer.scheduler_executer.execute(new SendFilesToDeleteMessage(peer));
			peer.scheduler_executer.execute(new SendGetFilesToDeleteMessage(peer));
		}

		
		Runtime.getRuntime().addShutdownHook(new Thread() 
	    { 
		      public void run() { 
		    	  try {
			            FileOutputStream fileOut =
			            new FileOutputStream("./peer" + peer.getId() + "/memory" + peer.getId() +  ".ser");
			            ObjectOutputStream out = new ObjectOutputStream(fileOut);
			            out.writeObject(peer.getMemory());
			            out.close();
						fileOut.close();
						if (peer.getVersion() == 2.0) {
							peer.getServerThread().close();
						}
			            System.out.printf("Serialized data of Peer " + peer.getId() + " has been saved in " + "./peer" + peer.getId() + "/memory" + peer.getId() +  ".ser");
			         } catch (IOException i) {
			            i.printStackTrace();
			         }
		      } 
		 });
		
		
	}
	
	
}