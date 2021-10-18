import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Superpeer implements ISuperpeer {
    //For each file it has a set of peers
    private ConcurrentHashMap<String, HashSet<String>> fileToClientIds = null;
    private HashMap<String,ISuperpeer> neighborToRemoteObject = null;
    private HashMap<String,IPeer> peerToRemoteObject = null;
    //private HashMap<String,String> messageIdToSender = null;
    //private HashMap<String,Integer> messageIdToResponsesLeft = null;
    private String id="";
    //Indexer constructor
    public Superpeer(String id, String[] neighbors) throws RemoteException {
        super();
        this.id=id;
        this.fileToClientIds = new ConcurrentHashMap<String,HashSet<String>>();
        neighborToRemoteObject = new HashMap<String,ISuperpeer>();
        //this.messageIdToSender = new HashMap<String,String>();
        //this.messageIdToResponsesLeft = new HashMap<String,Integer>();
        this.peerToRemoteObject = new HashMap<String,IPeer>();
        for (String neighbor:neighbors) {
            neighborToRemoteObject.put(neighbor,null);
        }
    }


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
            //System.out.println(this.fileToClientIds);
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

    //This method returns the set of peer with the specific file (sucess) or fail if the file is not in the server
    public synchronized HashMap<String,HashSet<String>> query(String messageID, Integer TTL, String fileName, String senderId,HashSet<String> visited) throws RemoteException {
        try{
            visited.add(this.id);
            System.out.println("Query called for message "+messageID+" and file "+fileName);
            HashMap<String,HashSet<String>> map = new HashMap<String,HashSet<String>>();
            HashSet<String> peersSet = this.fileToClientIds.get(fileName);
            map.put(this.id,peersSet);
            if(peersSet==null){
                peersSet=new HashSet<String>();
            }
            if(TTL==0){
                System.out.println("Broadcast stopped because TTL is equals to 0");
            }else{
                for(Map.Entry<String, ISuperpeer> entry : neighborToRemoteObject.entrySet()) {
                    String key = entry.getKey();
                    if(key.equals(senderId) || map.containsKey(key) || visited.contains(key)){
                        continue;
                    }
                    ISuperpeer value = entry.getValue();
                    if(value==null){
                        try{
                            String[] addPort = key.split(":");
                            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
                            ISuperpeer neighbor = (ISuperpeer) registry.lookup(key);
                            this.neighborToRemoteObject.put(key,neighbor);
                        }catch (Exception e){
                            System.out.println(key+" could not be located, ignoring it.");
                            continue;
                        }
                    }
                    map.putAll(this.neighborToRemoteObject.get(key).query(messageID,TTL-1,fileName,this.id,visited));
                    //peersSet.addAll();
                    //this.messageIdToResponsesLeft.put(messageID,this.messageIdToResponsesLeft.get(messageID)+1);
                    System.out.println(messageID+" sent to "+key);
                }
            }
            return  map;
//            if(isSenderPeer){
//                IPeer peer = null;
//                if(peerToRemoteObject.containsKey(senderId)){
//                    peer=peerToRemoteObject.get(senderId);
//                }else{
//                    String[] addPort = this.id.split(":");
//                    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
//                    peer = (IPeer) registry.lookup(senderId);
//                    this.peerToRemoteObject.put(senderId,peer);
//                }
//                this.messageIdToSender.put(messageID,senderId);
//                this.messageIdToResponsesLeft.put(messageID,0);
//                if (peersSet==null){
//                    peer.queryhit(messageID,fileName,new HashSet<String>(),this.id);
//                    System.out.println("File "+fileName+" was not in the superpeer. Empty query hit sent to "+senderId);
//                }else{
//                    peer.queryhit(messageID,fileName,peersSet,this.id);
//                    System.out.println("Query hit sent to "+senderId);
//                }
//            }else{
//                ISuperpeer superpeer=neighborToRemoteObject.get(senderId);
//                if(superpeer!=null){
//                    superpeer=neighborToRemoteObject.get(senderId);
//                }else{
//                    String[] addPort = senderId.split(":");
//                    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
//                    superpeer = (ISuperpeer) registry.lookup(senderId);
//                    this.neighborToRemoteObject.put(senderId,superpeer);
//                }
//                if(this.messageIdToSender.containsKey(messageID)){
//                    System.out.println("Message "+messageID+" already received, ignoring it.");
//                    superpeer.queryhit(messageID,fileName,null,this.id,true);
//                    return true;
//                }else{
//                    this.messageIdToSender.put(messageID,senderId);
//                    this.messageIdToResponsesLeft.put(messageID,0);
//                }
//                if (peersSet==null){
//                   superpeer.queryhit(messageID,fileName,new HashSet<String>(),this.id,false);
//                    System.out.println("File "+fileName+" was not in the superpeer. Empty query hit sent to "+senderId);
//                }else{
//                    superpeer.queryhit(messageID,fileName,peersSet,this.id,false);
//                    System.out.println("Query hit sent to "+senderId);
//                }
//            }
//            return null;
        }catch (Exception e){
            System.out.println("ERROR. Queryhit or broadcast failed.");
            e.printStackTrace();
            return  null;
        }
    }

//    public synchronized boolean queryhit(String messageID, String fileName, HashSet<String> set,String superPeer,boolean ignored) throws RemoteException {
//        System.out.println("Queryhit called for message "+messageID+" and file "+fileName);
//        this.messageIdToResponsesLeft.put(messageID,this.messageIdToResponsesLeft.get(messageID)-1);
//        String sentTo = "";
//        if(this.messageIdToResponsesLeft.get(messageID)==0){
//            this.messageIdToResponsesLeft.remove(messageID);
//            sentTo = this.messageIdToSender.get(messageID);
//            this.messageIdToSender.remove(messageID);
//        }
//        if(ignored){
//            return true;
//        }
//        if(peerToRemoteObject.containsKey(sentTo)){
//            peerToRemoteObject.get(sentTo).queryhit(messageID,fileName,set,superPeer);
//        }else{
//            neighborToRemoteObject.get(sentTo).queryhit(messageID,fileName,set,superPeer,false);
//        }
//        return true;
//
//    }

}
