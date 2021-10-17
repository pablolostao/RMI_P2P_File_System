import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

public interface IIndexer extends Remote{
    boolean registry(String peerId, String fileName) throws RemoteException;
    boolean deregister(String peerId, String fileName) throws RemoteException;
    HashSet<String> search(String fileName) throws RemoteException;
}
