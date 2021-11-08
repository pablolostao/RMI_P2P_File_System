import java.io.*;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;


public class PeerServer implements IPeer{
    //Id
    private String id;
    //Superpeer
    private String superpeer;
    //Path to owned files
    private Path owned_directory;
    //Path to downloaded files
    private Path downloaded_directory;
    //File to owned
    private ConcurrentHashMap<String,FileInfo> fileToInfo;
    //TTR in seconds
    private Integer TTR;
    //Invalidation policy
    private boolean isPull;

    public PeerServer(String id,String superpeer,ConcurrentHashMap<String,FileInfo> fileToInfo,Integer TTR,boolean isPull) throws RemoteException {
        super();
        this.id=id;
        this.fileToInfo=fileToInfo;
        this.superpeer=superpeer;
        this.TTR=TTR;
        this.isPull=isPull;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Boolean retry = true;
        //Init shared directory
        while(retry){
            try{
                System.out.println("Insert the path of the shared directory:");
                String dirPath = reader.readLine().trim();
                File dir = new File(dirPath);
                if (dir.isDirectory()){
                    Path owned_folder = dir.toPath().resolve("owned");
                    if(!Files.exists(owned_folder)){
                        owned_folder.toFile().mkdir();
                    }
                    Path downloaded_folder = dir.toPath().resolve("downloaded");
                    if(!Files.exists(downloaded_folder)){
                        downloaded_folder.toFile().mkdir();
                    }
                    this.owned_directory = owned_folder;
                    this.downloaded_directory = downloaded_folder;
                    File[] filesToRemove = this.downloaded_directory.toFile().listFiles();
                    if(filesToRemove !=null){
                        for (File child : filesToRemove) {
                            child.delete();
                        }
                    }
                    retry = false;
                }else{
                    System.out.println("The path is not a directory");
                }
            }catch (Exception e){
                System.out.println("Path not valid");
            }
        }
    }

    //Get owned directory
    public Path getOwned_directory() {
        return owned_directory;
    }

    //Get downloaded directory
    public Path getDownloaded_directory() {
        return downloaded_directory;
    }


    //Retrieve a file
    public synchronized RetrieveResponse retrieve(String fileName) throws RemoteException{
        File internalFile = null;
        if(this.fileToInfo.get(fileName).isOwned()){
            internalFile = this.owned_directory.resolve(fileName).toFile();
        }else{
            internalFile = this.downloaded_directory.resolve(fileName).toFile();
        }
        RetrieveResponse response = new RetrieveResponse();
        response.content = new byte [(int)internalFile.length()];
        response.fileInfo = this.fileToInfo.get(fileName);
        return response;
    }

    //Receives a queryhit and prints de result
    public synchronized void queryhit(String messageID, String fileName, HashSet<String> set,String superPeer) throws RemoteException{
        if(set.size()==0){
            return;
        }
        System.out.println("QUERYHIT - RECEIVED. Original sender: "+superPeer+" for "+fileName+". Set of peers:");
        System.out.println(set);
    }

    //Receives an invalidation message and prints it
    public synchronized void invalidation(String messageID, String fileName,Integer newVersion) throws RemoteException{
        try{
            this.fileToInfo.get(fileName).setValid(false);
            String[] addPort = this.superpeer.split(":");
            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
            ISuperpeer iSuperpeer = (ISuperpeer) registry.lookup(this.superpeer);
            new DeregisterThread(iSuperpeer,fileName,this.id).start();
            System.out.println("INVALIDATION - RECEIVED. File name: "+fileName+". New version: "+newVersion);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //Check if the input version is outdated
    public synchronized boolean isOutdated(String fileName,Integer version) throws RemoteException{
        try{
            if(this.fileToInfo.get(fileName).getVersion().equals(version)){
                System.out.println("IS OUTDATED - RECEIVED. File name: "+fileName+". Version: "+version+". Response: False");
                return false;
            }
            System.out.println("IS OUTDATED - RECEIVED. File name: "+fileName+". Version: "+version+". Response: True");
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return true;
        }
    }

    //Thread used for deregistering after invalidation
    private class DeregisterThread extends Thread{
        private String id;
        private String fileName;
        private ISuperpeer iSuperpeer;

        public DeregisterThread(ISuperpeer iSuperpeer,String fileName,String id){
            this.id=id;
            this.fileName=fileName;
            this.iSuperpeer=iSuperpeer;
        }
        public void run(){
            try{
                iSuperpeer.deregister(this.id,fileName);
            }catch (Exception e){
                System.out.println("Error deregistering after invalidation");
                e.printStackTrace();
            }

        }
    }

}
