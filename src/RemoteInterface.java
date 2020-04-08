import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    
    public String backup(String file_name, int replication) throws RemoteException; 

    public String restore(String file_name) throws RemoteException;

    public String delete(String file_name) throws RemoteException;

    public String manage(int newMemory) throws RemoteException;

    public String state() throws RemoteException;
}