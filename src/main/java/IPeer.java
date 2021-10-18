import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

public interface IPeer extends Remote{
    byte[] retrieve(String fileName) throws RemoteException;
    Path getShared_directory() throws RemoteException;
//    boolean queryhit(String messageID, String fileName, HashSet<String> set,String superPeer) throws RemoteException;
}
