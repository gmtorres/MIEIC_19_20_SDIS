import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javafx.util.Pair;

public class Memory implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static final String CRLF = String.valueOf((char)0xD) + String.valueOf((char)0xA) ;
	
	//InitPeer peer; 
	
	//counts the replications of the chunks it has sent to other peers
	// Id, perceived replication degree it has backuped
	private ConcurrentHashMap<String,Integer> chunkReplication = new ConcurrentHashMap<String,Integer>();
	
	//files data that have been backuped by this peer, contains path, fileId and desired replication
	private ArrayList<FileData> backupFiles = new ArrayList<FileData>();
	
	//Has the files that have been requested by the GETCHUNK
	private ConcurrentHashMap<String,Integer> requestedChunks = new ConcurrentHashMap<String,Integer>();

	//has all the chunks stored in this peer
	//Id , data , desiredReplication , perceivedReplication
	//private ConcurrentHashMap<String,Pair<byte[],Pair<Integer,Integer>>> chunkStored = new ConcurrentHashMap<String,Pair<byte[],Pair<Integer,Integer>>>();
	private ConcurrentHashMap<String,Pair<Integer,Integer>> chunkStored = new ConcurrentHashMap<String,Pair<Integer,Integer>>();
	
	
	//contains chunks that were deleted and the perceived replication is less than the desired
	private ArrayList<String> deletedChunksToSend = new ArrayList<String>();
	
	private ArrayList<Pair<String,Integer>> filesDeleted = new ArrayList<Pair<String,Integer>>();

	private FileInfo restoreFile = null;

	String path = "";
	
	private int maxMemory = 10000000; //10MB //expresso em bytes
	private int memoryInUse = 0;
	
	
	Memory(String p){
		path = p;
	}
	
	public void addDeletedChunk(String fileId) {
		deletedChunksToSend.add(fileId);
	}
	public void removeDeletedChunk(String fileId) {
		for(int i = 0; i < deletedChunksToSend.size(); i++) {
			if(deletedChunksToSend.get(i).equals(fileId))
				deletedChunksToSend.remove(i);
		}
	}
	public boolean isDeletedChunkToSend(String fileId) {
		for(int i = 0; i < deletedChunksToSend.size(); i++) {
			if(deletedChunksToSend.get(i).equals(fileId))
				return true;
		}
		return false;
	}
	
	
	public void addBackupFile(FileData fd) {
		backupFiles.add(fd);
	}
	
	
	public int getChunkReplication(String key) {
		Integer replication = chunkReplication.get(key);
		if(replication == null)
			return -1;
		return replication;
	}

	public int getRequestedChunk(String key) {
		Integer available = requestedChunks.get(key);
		if(available == null) {
			return -1;
		}
		return available;
	}

	public int isPeerRequesting(String key) {
		return requestedChunks.get(key);
	}

	public void eliminateRequestedChunk(String key) {
		requestedChunks.remove(key);
	}

	public void addRequestedChunk(String key) {
		requestedChunks.put(key,0);
	}

	public void addRequestedChunkPeer(String key) {
		requestedChunks.put(key,1);
	}
	
	public boolean addChunckReplication(String key) {
		//System.out.println(key);
		chunkReplication.put(key,0);
		return true;
	}
	public boolean addChunckReplication(String key, int current_rep) {
		//System.out.println(key);
		chunkReplication.put(key,current_rep);
		return true;
	}
	public void deleteChunckReplication(String key) {
		chunkReplication.remove(key);
	}
	
	public int incrementChunkReplication(String key) {
		Integer replication = chunkReplication.get(key);
		if(replication == null)
			return -1;
		replication++;
		chunkReplication.replace(key, replication);
		FileData fd = this.getFileDataByID(getFileIdFromKey(key));
		if(replication > fd.getPerceivedReplicationDegree())
			fd.setPerceivedReplicationDegree(replication);
		return replication;
	}
	
	public int decrementChunkReplication(String key) {
		Integer replication = chunkReplication.get(key);
		if(replication == null)
			return -1;
		replication--;
		chunkReplication.replace(key, replication);
		FileData fd = this.getFileDataByID(getFileIdFromKey(key));
		fd.setPerceivedReplicationDegree(getMaximumPerceivedReplicationDegree(fd.getFileId(),fd.getNumberOfChunks()));
		return replication;
	}
	
	private String getFileIdFromKey(String k) {
		String [] t = k.split("_");
		return t[0];
	}
	
	private int getMaximumPerceivedReplicationDegree(String file_id, int chunkNo) {
		int r = 0;
		for(int i = 0; i < chunkNo;i++) {
			int t = this.chunkReplication.get(file_id+"_" + i).intValue();
			if(t > r) {
				r = t;
			}
		}
		return r;
	}
	
	
	public boolean storeChunk(String key, byte[] data , int replication){	
		//if(chunkStored.get(key) != null)
			//return;
		int newMemory = memoryInUse + data.length;
		if(newMemory > this.maxMemory) return false; //sem espaço
		
		try (FileOutputStream fileOuputStream = new FileOutputStream(this.path + key)) {
            fileOuputStream.write(data);
            Pair<Integer,Integer> reps = new Pair<>(replication,1);
    		chunkStored.put(key, reps);
    		memoryInUse += data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
		return true;
	}
	public boolean isChunkStored(String key) {
		return chunkStored.get(key) != null;
	}
	public void incrementChunkPerceivedReplication(String key) {
		Pair<Integer,Integer>  pair= chunkStored.get(key);
		if(pair == null) return;
		Pair<Integer,Integer> rep = pair;
		Pair<Integer,Integer> newPair = new Pair<>(rep.getKey(),rep.getValue()+1);
		chunkStored.replace(key, newPair);
	}
	public void decrementChunkPerceivedReplication(String key) {
		Pair<Integer,Integer>  pair= chunkStored.get(key);
		if(pair == null) return;
		Pair<Integer,Integer> rep = pair;
		Pair<Integer,Integer> newPair = new Pair<>(rep.getKey(),rep.getValue()-1);
		chunkStored.replace(key, newPair);
	}
	public int getStoredChunkDesiredReplication(String key) {
		Pair<Integer,Integer>  pair= chunkStored.get(key);
		if(pair == null) return -1;
		return pair.getKey();
	}
	public int getStoredChunkPerceivedReplication(String key) {
		Pair<Integer,Integer>  pair= chunkStored.get(key);
		if(pair == null) return -1;
		return pair.getValue();
	}
	
	public void removeStoredChunk(String key) {
		File file = new File(path + key);
		memoryInUse -= file.length();
		file.delete();
		chunkStored.remove(key);
		//memoryInUse -= chunkStored.remove(key).getKey().length;
	}
	
	
	
	public FileData getFileData(String file_path) {
		for(FileData fd : backupFiles) {
			if(fd.getFilepath().equals(file_path))
				return fd;
		}
		return null;
	}
	
	public FileData getFileDataByID(String fileId) {
		for(FileData fd : backupFiles) {
			if(fd.getFileId().equals(fileId))
				return fd;
		}
		return null;
	}
	
	
	public boolean removeFile(String id) { 
		//remove replication information of chunks sent to other peers
		for(int i = 0; chunkReplication.containsKey(id+"_"+i);++i) {
			chunkReplication.remove(id+"_"+i);
		}
		//removes files info that were initiated by this peer
		for(int i = 0; i < backupFiles.size();++i) {
			if(backupFiles.get(i).getFileId().equals(id)) {
				backupFiles.remove(i);	
				break;
			}
		}
		
		boolean had_file = false;
		Set<ConcurrentHashMap.Entry<String,Pair<Integer,Integer>>> entrySet = chunkStored.entrySet();
		for(Iterator<ConcurrentHashMap.Entry<String,Pair<Integer,Integer>>> itr = entrySet.iterator(); itr.hasNext();) {
			ConcurrentHashMap.Entry<String,Pair<Integer,Integer>> entry = itr.next();
			if(this.getFileIdFromKey(entry.getKey()).equals(id)) {
				removeStoredChunk(entry.getKey()); //removes chunk with given Id while subtracting its occupied memory
				had_file = true;
			}
		}
		return had_file;
	}
	
	
	public void reclaimMemory(int newMemory,InitPeer peer) {
		maxMemory = newMemory;
		if(maxMemory >= memoryInUse) { //se a restriï¿½ï¿½o de memoria nï¿½o altera o que estï¿½ guardado
			return;
		}
		
		Set<ConcurrentHashMap.Entry<String,Pair<Integer,Integer>>> entrySet = chunkStored.entrySet();
		for(Iterator<ConcurrentHashMap.Entry<String,Pair<Integer,Integer>>> itr = entrySet.iterator(); memoryInUse > maxMemory && itr.hasNext();) {
			ConcurrentHashMap.Entry<String,Pair<Integer,Integer>> entry = itr.next();
			if(entry.getValue().getValue() > entry.getValue().getKey()) { // perceived > desired 
				String key = entry.getKey();
				removeStoredChunk(key);
				System.out.println("Removed chunk: " + key);
				String [] chunkInfo = key.split("_");
				String message = peer.getVersion() + " REMOVED " + peer.getId() + " " + chunkInfo[0] + " " + chunkInfo[1] + " " + CRLF + CRLF;
				peer.getControlChannel().sendMessage(message.getBytes());
			}
		}
		
		while(memoryInUse > maxMemory) {
			
			ConcurrentHashMap.Entry<String,Pair<Integer,Integer>> max_entry = null;
			
			for(Iterator<ConcurrentHashMap.Entry<String,Pair<Integer,Integer>>> itr = entrySet.iterator();itr.hasNext();) {
				ConcurrentHashMap.Entry<String,Pair<Integer,Integer>> entry = itr.next();
				if(max_entry == null || entry.getValue().getValue() > max_entry.getValue().getValue()) { // perceived > desired 
					max_entry = entry;
				}
			}
			if(max_entry == null)
				break;
			String key = max_entry.getKey();
			removeStoredChunk(key);
			System.out.println("Removed chunk: " + key);
			String [] chunkInfo = key.split("_");
			String message = peer.getVersion() + " REMOVED " + peer.getId() + " " + chunkInfo[0] + " " + chunkInfo[1] + " " + CRLF + CRLF;
			peer.getControlChannel().sendMessage(message.getBytes());
		}
		
	}
	
	
	public void addFileDeleted(String fileId, int percievedRep) {
		this.filesDeleted.add(new Pair<String,Integer>(fileId,percievedRep));
	}
	
	public int getFileDeletedPerceived(String fileId) {
		for(Pair<String,Integer> p : this.filesDeleted) {
			if(p.getKey().equals(fileId))
				return p.getValue();
		}
		return 0;
	}
	public boolean isFileInFileDeleted(String fileId) {
		for(Pair<String,Integer> p : this.filesDeleted) {
			if(p.getKey().equals(fileId))
				return true;
		}
		return false;
	}
	public boolean decrementFileDeletePerceived(String fileId) {
		for(Pair<String,Integer> p : this.filesDeleted) {
			if(p.getKey().equals(fileId)) {
				this.filesDeleted.remove(p);
				if(p.getValue() - 1 > 0) {
					this.filesDeleted.add(new Pair<String,Integer>(p.getKey(),p.getValue()-1));
					return false;
				}else return true;
			}
		}
		return true;
	}
	
	public ArrayList<Pair<String,Integer>> getFileDeletedList() {
		/*String str = "";
		for(Pair<String,Integer> p : this.filesDeleted) {
			str+=p.getKey() + " ";
		}
		return str;*/
		ArrayList<Pair<String,Integer>> l = new ArrayList<Pair<String,Integer>>();
		for(Pair<String,Integer> p : this.filesDeleted) {
			l.add(p);
		}
		return l;
	}
	
	
	
	
	public String getBackupedFile() {
		String str = "";
		str+="Files that whose backup was initiated by this peer:\n";
		str+="FILE ID          DESIRED REP\n\n";
		for(int i = 0; i < backupFiles.size() ; ++i) {
			FileData fd = backupFiles.get(i);
			str += fd.getFilepath() + "  " + fd.getFileId() + "  " + fd.getReplicationDegree() + "\n";
			str +="\tCHUNK ID         PERCEIVED REP\n";
			for(int ii = 0; ii < fd.getNumberOfChunks() ; ++ii) {
				String key = fd.getFileId() + "_" + ii;
				int rep = this.getChunkReplication(key);
				if(rep != -1) {
					str += "\t-> " + key + "  " + rep + "\n";
				}
			}
		}
		if(backupFiles.size() == 0) str+="\nNo files\n";
		str+="\n\nChunks stored in this peer: \n";
		str+="CHUNK ID       DESIRED REP     PERCEIVED REP\n\n";
		for(ConcurrentHashMap.Entry<String,Pair<Integer,Integer>> entry : chunkStored.entrySet()) {
			str+=entry.getKey() + " " + entry.getValue().getKey() + " " + entry.getValue().getValue() + "\n";
		}
		if(chunkStored.isEmpty()) {
			str+="\nNo files\n";
		}
		str+="\n\nGeneral info: Max memory: " + this.maxMemory + "\tMemory in use: " + this.memoryInUse;
		return str;
	}

	public byte[] getChunkData(String key) {
		byte[] buf = null;
		File file = new File(path + key);
		try(FileInputStream fileInputStream = new FileInputStream(file);){
			buf = new byte[(int) file.length()];
			fileInputStream.read(buf);
		} catch (IOException e) {
            e.printStackTrace();
        } 
		return buf;
	}


	public void changeRestoreFile(FileInfo file) {
		restoreFile = file;
	}

	public void eliminateRestoreFile() {
		restoreFile = null;
	}

	public FileInfo getRestoreFile() {
		return restoreFile;
	}

	
	
}
