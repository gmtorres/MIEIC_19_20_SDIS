import java.io.Serializable;

public class FileData implements Serializable {
	
	private static final long serialVersionUID = 1L;

	
	private String filepath;
	private String filename;
	private String fileId;
	private int desiredReplicationDegree;
	private int percievedReplicationDegree;
	
	private int numberOfChunks;
	
	FileData(String fp, String fi , int rep, int n,String fn){
		filepath = fp;
		fileId = fi;
		desiredReplicationDegree = rep;
		numberOfChunks = n;
		percievedReplicationDegree = 0;
		this.filename = fn;
	}

	public String getFilepath() {
		return filepath;
	}

	public int getReplicationDegree() {
		return desiredReplicationDegree;
	}
	
	public int getPerceivedReplicationDegree() {
		return percievedReplicationDegree;
	}
	public void setPerceivedReplicationDegree(int r) {
		this.percievedReplicationDegree = r;
	}

	public String getFileId() {
		return fileId;
	}

	public int getNumberOfChunks() {
		return numberOfChunks;
	}
	
	public void setFileName(String n) {
		this.filename = n;
	}
	
	public String getFileName() {
		return this.filename;
	}
	
}
