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
    //Map from neighborID to neighbor remote object (only superpeers)
    private HashMap<String,ISuperpeer> neighborToRemoteObject = null;
    //Map from peerID to remote object
    private HashMap<String,IPeer> peerToRemoteObject = null;
    //Map from messageID to sender (to be able to recreate the path)
    private HashMap<String,String> messageIdToSender = null;
    //Superpeer id
    private String id="";

    public Superpeer(String id, String[] neighbors) throws RemoteException {
        super();
        this.id=id;
        this.fileToClientIds = new ConcurrentHashMap<String,HashSet<String>>();
        neighborToRemoteObject = new HashMap<String,ISuperpeer>();
        this.messageIdToSender = new HashMap<String,String>();
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

    //This method returns the set of peers in the same superpeer with that file and broadcasts the query to all neighbors except the query sender
    public synchronized void query(String messageID, Integer TTL, String fileName, String senderId) throws RemoteException {
        try{
            System.out.println("QUERY - RECEIVED from "+senderId+" for "+fileName+" ("+messageID+") ");
            //Prevent map of messagesID to sender to grow infinitely
            String[] messageIDParams = messageID.split("-");
            Integer prevSeqNumber = Integer.parseInt(messageIDParams[2])-1;
            String prevMessageID = messageIDParams[0]+"-"+messageIDParams[1]+"-"+prevSeqNumber.toString();
            if(this.messageIdToSender.containsKey(prevMessageID)){
                this.messageIdToSender.remove(prevMessageID);
            }
            //Set of peers that own the file
            HashSet<String> peersSet = this.fileToClientIds.get(fileName);
            //If sender is a peer
            if(!this.neighborToRemoteObject.containsKey(senderId)){
                //Locate remote object
                IPeer peer = null;
                if(peerToRemoteObject.containsKey(senderId)){
                    peer=peerToRemoteObject.get(senderId);
                }else{
                    String[] addPort = this.id.split(":");
                    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
                    peer = (IPeer) registry.lookup(senderId);
                    this.peerToRemoteObject.put(senderId,peer);
                }
                //Set sender of the message
                this.messageIdToSender.put(messageID,senderId);
                //Send query hit
                if(peersSet!=null){
                    new QueryHitToPeer(peer,messageID,fileName,peersSet,this.id).start();
                    System.out.println("QUERYHIT - SENT to "+senderId+" for "+fileName+" ("+messageID+") ");
                }
            //If sender is a superpeer
            }else{
                //Locate remote object
                ISuperpeer superpeer=neighborToRemoteObject.get(senderId);
                if(superpeer==null){
                    String[] addPort = senderId.split(":");
                    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
                    superpeer = (ISuperpeer) registry.lookup(senderId);
                    this.neighborToRemoteObject.put(senderId,superpeer);
                }
                // If we had received the message from another superpeer, we ignore it
                if(this.messageIdToSender.containsKey(messageID)){
                    System.out.println("Message "+messageID+" already received, ignoring it.");
                    return;
                }
                //Set message sender
                this.messageIdToSender.put(messageID,senderId);
                //Send query hit
                if (peersSet!=null){
                    new QueryHitToSuperPeer(superpeer,messageID,fileName,peersSet,this.id,this.id).start();
                    System.out.println("QUERYHIT - SENT to "+senderId+" for "+fileName+" ("+messageID+") ");
                }
            }
            // If TTL is equals to zero, stop broadcast
            if(TTL==0){
                System.out.println("Broadcast stopped because TTL is equals to 0");
            }else{
                //For each neighbor, try to locate it and create a thread to broadcast the query
                for(Map.Entry<String, ISuperpeer> entry : neighborToRemoteObject.entrySet()) {
                    String key = entry.getKey();
                    // Do not broadcast to the sender
                    if(key.equals(senderId)){
                        continue;
                    }
                    ISuperpeer neighbor = entry.getValue();
                    if(neighbor==null){
                        try{
                            String[] addPort = key.split(":");
                            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
                            neighbor = (ISuperpeer) registry.lookup(key);
                            this.neighborToRemoteObject.put(key,neighbor);
                        //Neighbor crashed
                        }catch (Exception e){
                            System.out.println(key+" could not be located, ignoring it.");
                            continue;
                        }
                    }
                    //Create thread
                    new QueryBroadcast(neighbor,messageID,TTL-1,fileName,this.id,key).start();
                    System.out.println("QUERY - SENT to "+key+" for "+fileName+" ("+messageID+") ");
                }
            }
        }catch (Exception e){
            System.out.println("ERROR. Queryhit or broadcast failed.");
            e.printStackTrace();
        }
    }

    //Receives an invalidation message and broadcasts it
    public synchronized  void invalidation(String messageID, Integer TTL,String fileName, Integer newVersion,String senderId) throws RemoteException{
        if(this.messageIdToSender.containsKey(messageID)){
            System.out.println("Message "+messageID+" already received, ignoring it.");
            return;
        }
        System.out.println("INVALIDATION - RECEIVED for "+fileName+". New version: "+newVersion.toString());
        HashSet<String> peersSet = this.fileToClientIds.get(fileName);
        if(peersSet!=null){
            for(String peer:peersSet){
                if(peer.equals(senderId)){
                    continue;
                }
                IPeer peerObject = this.peerToRemoteObject.get(peer);
                peerObject.invalidation(messageID,fileName,newVersion);
            }
        }
        //Set message sender
        this.messageIdToSender.put(messageID,senderId);
        // If TTL is equals to zero, stop broadcast
        if(TTL==0){
            System.out.println("Broadcast stopped because TTL is equals to 0");
        }else{
            //For each neighbor, try to locate it and create a thread to broadcast the query
            for(Map.Entry<String, ISuperpeer> entry : neighborToRemoteObject.entrySet()) {
                String key = entry.getKey();
                // Do not broadcast to the sender
                if(key.equals(senderId)){
                    continue;
                }
                ISuperpeer neighbor = entry.getValue();
                if(neighbor==null){
                    try{
                        String[] addPort = key.split(":");
                        Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
                        neighbor = (ISuperpeer) registry.lookup(key);
                        this.neighborToRemoteObject.put(key,neighbor);
                        //Neighbor crashed
                    }catch (Exception e){
                        System.out.println(key+" could not be located, ignoring it.");
                        continue;
                    }
                }
                //Create thread
                new InvalidationBroadcast(this.id,neighbor,messageID,TTL-1,fileName,newVersion).start();
                System.out.println("INVALIDATION - SENT to "+key+" for "+fileName+" ("+messageID+") ");
            }
        }

    }

    //This method receives a queryhit and forwards it to corresponding peer/superpeer
    public synchronized void queryhit(String messageID, String fileName, HashSet<String> set,String superPeer,String queryHitSender) throws RemoteException {
        try{
            System.out.println("QUERYHIT - RECEIVED from "+queryHitSender+". Original sender: "+superPeer+" for "+fileName+" ("+messageID+") ");
            //To know who we should forward to
            String sentTo = this.messageIdToSender.get(messageID);
            //If it is the peer
            if(peerToRemoteObject.containsKey(sentTo)){
                new QueryHitToPeer(peerToRemoteObject.get(sentTo),messageID,fileName,set,superPeer).start();
            //If it is a superpeer
            }else{
                new QueryHitToSuperPeer(neighborToRemoteObject.get(sentTo),messageID,fileName,set,superPeer,this.id).start();
            }
            System.out.println("QUERYHIT - FORWARDED to "+sentTo+". Original sender: "+superPeer+" for "+fileName+" ("+messageID+") ");
        }catch (Exception e){
            System.out.println("Error. Queryhit for message "+messageID);
            e.printStackTrace();
        }
    }

    //Thread used for broadcasting query messages
    private class QueryBroadcast extends Thread{
        private String id;
        private String target;
        private String fileName;
        private Integer TTL;
        private String messageID;
        private ISuperpeer neighbor;


        public QueryBroadcast(ISuperpeer neighbor,String messageID, Integer TTL,String fileName,String id, String target){
            this.id=id;
            this.fileName=fileName;
            this.neighbor=neighbor;
            this.TTL=TTL;
            this.messageID=messageID;
            this.target = target;
        }
        public void run(){
            try{
                neighbor.query(messageID,TTL,fileName,id);
            }catch (Exception e){
                System.out.println("Error in broadcast");
                e.printStackTrace();
            }

        }
    }

    //Thread used for broadcasting invalidation messages
    private class InvalidationBroadcast extends Thread{
        private String id;
        private String fileName;
        private Integer TTL;
        private Integer newVersion;
        private String messageID;
        private ISuperpeer neighbor;


        public InvalidationBroadcast(String id,ISuperpeer neighbor,String messageID, Integer TTL,String fileName,Integer newVersion){
            this.id = id;
            this.newVersion=newVersion;
            this.fileName=fileName;
            this.neighbor=neighbor;
            this.TTL=TTL;
            this.messageID=messageID;
        }
        public void run(){
            try{
                neighbor.invalidation(messageID,TTL,fileName,newVersion,this.id);
            }catch (Exception e){
                System.out.println("Error in broadcast");
                e.printStackTrace();
            }

        }
    }

    //Thread used to send queryhit to the peer
    private class QueryHitToPeer extends Thread{
        IPeer peer =null;
        String messageID=null;
        String fileName=null;
        HashSet<String> set=null;
        String superPeer=null;

        public QueryHitToPeer(IPeer peer,String messageID,String fileName,HashSet<String> set,String superPeer){
            this.peer=peer;
            this.messageID=messageID;
            this.superPeer=superPeer;
            this.set=set;
            this.fileName=fileName;
        }
        public void run(){
            try{
                peer.queryhit(messageID,fileName,set,superPeer);
            }catch (Exception e){
                System.out.println("Error in queryhit to peer");
                e.printStackTrace();
            }

        }
    }

    //Thread used to send queryhit to a superpeer
    private class QueryHitToSuperPeer extends Thread{
        ISuperpeer superpeer =null;
        String messageID=null;
        String fileName=null;
        HashSet<String> set=null;
        String superPeer=null;
        String id;

        public QueryHitToSuperPeer(ISuperpeer superpeer,String messageID,String fileName,HashSet<String> set,String superPeer,String id){
            this.superpeer=superpeer;
            this.messageID=messageID;
            this.superPeer=superPeer;
            this.set=set;
            this.fileName=fileName;
            this.id=id;
        }
        public void run(){
            try{
                superpeer.queryhit(messageID,fileName,set,superPeer,this.id);
            }catch (Exception e){
                System.out.println("Error in queryhit to peer");
                e.printStackTrace();
            }

        }
    }

}
