import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;

//Interface for superpeers
public interface ISuperpeer extends Remote{
    //Registers a specific file for a specific peer
    boolean registry(String peerId, String fileName) throws RemoteException;
    //Deregisters a specific file for a specific peer
    boolean deregister(String peerId, String fileName) throws RemoteException;
    //Returns the set of peers in the same superpeer with that file
    HashSet<String> search(String fileName) throws RemoteException;
    //Returns the set of peers in the same superpeer with that file and broadcasts the query to all neighbors except the query sender
    void query(String messageID, Integer TTL, String fileName, String senderId) throws RemoteException;
    //Receives a queryhit and forwards it to corresponding peer/superpeer
    void queryhit(String messageID, String fileName, HashSet<String> set,String superPeer,String queryHitSender) throws RemoteException;
    //Receives an invalidation message and broadcasts it
    void invalidation(String messageID, Integer TTL,String fileName, Integer newVersion,String senderId) throws RemoteException;
}
