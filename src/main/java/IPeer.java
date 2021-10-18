import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

//Interface for peers
public interface IPeer extends Remote{
    //Retrieve a file
    byte[] retrieve(String fileName) throws RemoteException;
    //Returns shared directory
    Path getShared_directory() throws RemoteException;
    //Receives a queryhit and prints de result
    void queryhit(String messageID, String fileName, HashSet<String> set,String superPeer) throws RemoteException;
}
