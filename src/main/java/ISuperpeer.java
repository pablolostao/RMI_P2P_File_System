import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;

public interface ISuperpeer extends Remote{
    boolean registry(String peerId, String fileName) throws RemoteException;
    boolean deregister(String peerId, String fileName) throws RemoteException;
    HashSet<String> search(String fileName) throws RemoteException;
    HashMap<String,HashSet<String>> query(String messageID, Integer TTL, String fileName, String senderId,HashSet<String> visited) throws RemoteException;
//    boolean queryhit(String messageID, String fileName, HashSet<String> set,String superPeer,boolean ignored) throws RemoteException;
}
