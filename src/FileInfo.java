import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileInfo implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<byte[]> fileParts;
    private File file;
    private AtomicInteger numChunksWritten = new AtomicInteger(0);
    private AtomicInteger numChunksBackedup = new AtomicInteger(0);
    private AtomicInteger backupState = new AtomicInteger(0); // 0 = valid; 1 = partial failure; 2 = complete failure
    private AtomicInteger restoreState = new AtomicInteger(0); // 0 = restoring ; 1 = success; 2 =  failure

    private FileData fileData;

    public FileInfo(String path, int replicationDegree) {
        this.file = new File(path);
        this.fileParts = new ArrayList<byte[]>();
        if(this.doesFileExists())
        	fileDivision();

        this.fileData = new FileData(path, createFileId(), replicationDegree, this.fileParts.size(),this.file.getName());
    }

    public FileInfo(FileData fileData) {
        this.file = null;
        this.fileData = fileData;

        this.fileParts = new ArrayList<byte[]>(fileData.getNumberOfChunks());
        
        System.out.println("No of chunks:" + fileData.getNumberOfChunks());
        for (int i = 0; i < fileData.getNumberOfChunks(); i++) {
            this.fileParts.add(null);
        }
    }
    public boolean doesFileExists() {
    	return this.file.exists();
    }

    public void setFile(File file) {
        this.file = file;
    }

    public FileData getFileData() {
        return this.fileData;
    }

    public String getFileId() {
        return this.fileData.getFileId();
    }

    public int getReplicationDegree() {
        return this.fileData.getReplicationDegree();
    }

    public ArrayList<byte[]> getFileParts() {
        return this.fileParts;
    }

    public int getNumberOfParts() {
        return this.fileParts.size();
    }

    public byte[] getFilePart(int i) {
        return this.fileParts.get(i);
    }

    private String createFileId() {

        Long date = this.file.lastModified();
        String modifiedDate = Long.toString(date);

        String fileString = this.file.getName() + modifiedDate + this.file.getParent();

        fileString = sha256(fileString);

        return fileString;
    }

    public void setFilePart(int chunkNo, byte[] chunk) {
    	if(this.getFilePart(chunkNo) == null) { // se não estiver nada escrito lá
    		fileParts.set(chunkNo, chunk);
            this.increaseWritten();
            this.compareChunkNumber();
    	}else {
    		//System.out.println("Chunk " + chunkNo + " already stored");
    	}
    }

    public int checkFlag() {
        return this.restoreState.get();
    }

    public void increaseWritten() {
        this.numChunksWritten.incrementAndGet();
    }

    public int getWritten() {
        return this.numChunksWritten.get();
    }
    
    public void restoreFailed() {
    	this.restoreState.set(2);
    }

    public void activateFlag() {
    	this.restoreState.set(1);
    }

    public void compareChunkNumber() {
    	System.out.println( this.fileData.getFilepath() + ": " +  this.numChunksWritten + " of " + this.fileData.getNumberOfChunks());
        if (this.getWritten() == this.fileData.getNumberOfChunks()) {
        	this.activateFlag();
        }
    }

    private void fileDivision() {

        int divSize = 64000;
        byte[] buf = new byte[divSize];

        try (FileInputStream inputStream = new FileInputStream(this.file);
                BufferedInputStream bufInputStream = new BufferedInputStream(inputStream)) {

            int bytesAmount = 0;

            while ((bytesAmount = bufInputStream.read(buf, 0, divSize)) > 0) {
                byte[] newBuf = Arrays.copyOf(buf, bytesAmount);
                this.fileParts.add(newBuf);
                buf = new byte[divSize];
            }

            if ((this.file.length() % divSize) == 0) {
                this.fileParts.add(new byte[0]);
            }

        }

        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String sha256(String fileString) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(fileString.getBytes(StandardCharsets.UTF_8));

            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < encodedhash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "null";

    }

    public File createFile(String path, String dir) {
        File file = new File(dir + path);
        System.out.println("Writing into " + dir + path);
        try(OutputStream os = new FileOutputStream(file);) {
            for(int t = 0; t < this.fileData.getNumberOfChunks(); t++) {
            	byte [] data = this.getFilePart(t);
            	if(data == null) System.out.println("DATA is NULL");
            	else os.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }
    
    
    public void incrementChunksBackedup() {
    	this.numChunksBackedup.incrementAndGet();
    }
    public void setFileState(int v) {
    	this.backupState.set(v);
    }
    public int getChunksBackedup() {
    	return this.numChunksBackedup.get();
    }
    public int getBackupState() {
    	return this.backupState.get();
    }
    
    public void setRestoreState(int v) {
    	this.restoreState.set(v);
    }

    public int getRestoreState() {
    	return this.restoreState.get();
    }


}