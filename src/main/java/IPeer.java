import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

//Interface for peers
public interface IPeer extends Remote{
    //Retrieve a file
    RetrieveResponse retrieve(String fileName) throws RemoteException;
    //Returns owned directory
    Path getOwned_directory() throws RemoteException;
    //Returns downloaded directory
    Path getDownloaded_directory() throws RemoteException;
    //Receives a queryhit and prints de result
    void queryhit(String messageID, String fileName, HashSet<String> set,String superPeer) throws RemoteException;
    //Receives an invalidation message and prints it
    void invalidation(String messageID, String fileName, Integer newVersion) throws RemoteException;
    //Check if a specific version for a specific file is outdated
    boolean isOutdated(String fileName,Integer version) throws RemoteException;
}
