import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class Superpeer implements IIndexer{
    //For each file it has a set of peers
    private ConcurrentHashMap<String, HashSet<String>> fileToClientIds = null;
    //Indexer constructor
    public Superpeer() throws RemoteException {
        super();
        this.fileToClientIds = new ConcurrentHashMap<String,HashSet<String>>();
    }

//    public static void main(String[] args) {
//        try {
//            String name = "127.0.0.1:30301";
//            String[] addPort = name.split(":");
//            IIndexer iIndexer = new Indexer();
//            IIndexer stub = (IIndexer) UnicastRemoteObject.exportObject(iIndexer, Integer.parseInt(addPort[1]));
//            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(addPort[1]));
//            registry.rebind(name,stub);
//            System.out.println("Indexer bound");
//        } catch (Exception e) {
//            System.err.println("Indexer exception:");
//            e.printStackTrace();
//        }
//    }


    //This method registers a specific file for a specific peer
    //Returns success if it has been added correctly or if it was already added
    public synchronized boolean registry(String peerId, String fileName) throws RemoteException {
        try{
            System.out.println("Registry called for file "+fileName+" and peer "+peerId);
            String[] addPort = peerId.split(":");
            //If there is no position 1 or it is not parseable, exception
            Integer.parseInt(addPort[1]);
            //If the file does not exist, init its set
            if (!fileToClientIds.containsKey(fileName)) {
                fileToClientIds.put(fileName,new HashSet<String>());
            }
            //Register peer
            fileToClientIds.get(fileName).add(peerId);
            //Respond
            System.out.println("File "+fileName+" registered successfully for peer "+peerId);
            System.out.println(this.fileToClientIds);
            return true;
        }catch(Exception e){
            System.out.println("ERROR. File "+fileName+" could not be registered for peer "+peerId);
            e.printStackTrace();
            return false;
        }
    }

    //This method deregisters a specific file for a specific peer
    //Returns success if the file was not registered or if it has been deregistered correctly
    public synchronized boolean deregister(String peerId, String fileName) throws RemoteException {
        try{
            System.out.println("Deregister called for file "+fileName+" and peer "+peerId);
            HashSet<String> peersSet = this.fileToClientIds.get(fileName);
            //If the file is not in the server, success
            if (peersSet == null) {
                System.out.println("File "+fileName+" was not in the server");
                return true;
            }
            //If the file does not have that peer registered, success
            if (!peersSet.contains(peerId)){
                System.out.println("File "+fileName+" was not in the server for peer "+peerId);
                return true;
            }
            //If deregistration, success
            peersSet.remove(peerId);
            System.out.println("File "+fileName+" deregistered successfully for peer "+peerId);
            return true;
        }catch(Exception e){
            //If error, return error
            System.out.println("ERROR. File "+fileName+" could not be deregistered for peer "+peerId);
            return false;
        }
    }

    //This method returns the set of peer with the specific file (sucess) or fail if the file is not in the server
    public synchronized HashSet<String> search(String fileName) throws RemoteException {
        try{
            System.out.println("Search called for file "+fileName);
            HashSet<String> peersSet = this.fileToClientIds.get(fileName);
            //If the file is not in the server, fail
            if (peersSet==null){
                System.out.println("File "+fileName+" was not in the server");
                this.fileToClientIds.put(fileName,new HashSet<String>());
                return this.fileToClientIds.get(fileName);
            }
            System.out.println("Peers set returned successfully");
            return peersSet;
        }catch (Exception e){
            System.out.println("ERROR. Peers set could not be returned successfully");
            return null;
        }
    }

}
