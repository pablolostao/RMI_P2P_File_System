import java.io.*;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;

public class PeerClient extends Thread{
    //Peer id
    private String id;
    //Corresponding superpeer id
    private String superpeer;
    //Path to owned directory
    private Path owned_directory;
    //Path to downloaded directory
    private Path downloaded_directory;
    //Counter of sent messages to create messageID
    private Integer nextMessageID;
    //File to owned
    ConcurrentHashMap<String,FileInfo> fileToInfo;
    //TTR in seconds
    Integer TTR;
    //Invalidation policy
    boolean isPull;

    public PeerClient(String id, String superpeer, Path owned_directory,Path downloaded_directory,ConcurrentHashMap<String,FileInfo> fileToInfo,Integer TTR,boolean isPull){
        this.superpeer =superpeer;
        this.id =id;
        this.owned_directory=owned_directory;
        this.downloaded_directory=downloaded_directory;
        this.nextMessageID=0;
        this.fileToInfo=fileToInfo;
        this.isPull=isPull;
        this.TTR=TTR;
    }

    //Thread to manage user inputs
    public void run() {
        try{
            //Locate corresponding superpeer
            String[] addPort = this.superpeer.split(":");
            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
            ISuperpeer iSuperpeer = (ISuperpeer) registry.lookup(this.superpeer);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            Boolean retry = true;
            //Register all files in shared directory
            File[] files = this.owned_directory.toFile().listFiles();
            if(files !=null){
                for (File child : files) {
                    boolean success = iSuperpeer.registry(this.id, child.getName());
                    if(success){
                        System.out.println("File "+child.getName()+" registered successfully for peer "+this.id);
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setOwned(true);
                        fileInfo.setOriginServer(this.id);
                        fileInfo.setValid(true);
                        fileInfo.setVersion(1);
                        fileInfo.setTTR(this.TTR);
                        fileInfo.setOriginSuperpeer(this.superpeer);
                        this.fileToInfo.put(child.getName(),fileInfo);
                    }else{
                        System.out.println("ERROR. File "+child.getName()+" could not be registered for peer "+this.id);
                    }
                }
            }
            //Create another thread to watch directory events
            new Watcher(this.id, iSuperpeer,this.owned_directory,this.nextMessageID,this.superpeer,this.fileToInfo,this.isPull).start();
            //Get user inputs and execute actions
            int choice = 0;
            while(true){
                retry = true;
                while (retry){
                    try{
                        System.out.println("Choose action");
                        System.out.println("[1] To look for alternatives to download a file");
                        System.out.println("[2] To download a specific file from specific peer");
                        System.out.println("[3] Refresh outdated file");
                        System.out.println("[4] To exit");
                        String line = reader.readLine().trim();
                        choice = Integer.parseInt(line);
                        retry = false;
                    }catch (NumberFormatException e){
                        System.out.println("Choice not valid");
                    }
                }
                switch (choice) {
                    //We request to the indexer information about one file
                    case 1:
                        System.out.println("\nEnter the name of the file you are looking for:");
                        String name = reader.readLine().trim();
                        iSuperpeer.query(this.superpeer+"-"+this.id+"-"+nextMessageID.toString()+"-(Q)",4,name,this.id);
                        this.nextMessageID=this.nextMessageID+1;
                        break;
                    //We request a file directly to another peer
                    case 2:
                        //Peer, corresponding superpeer and name file needed
                        System.out.println("\nEnter the name of the file you are looking for:");
                        String fileName = reader.readLine().trim();
                        System.out.println("\nEnter the peer you want to download from (address:port):");
                        String peerName = reader.readLine().trim();
                        System.out.println("\nEnter corresponding superpeer:");
                        String superpeer = reader.readLine().trim();
                        //Locate peer
                        Registry assRegistry = LocateRegistry.getRegistry(Integer.parseInt(superpeer.split(":")[1]));
                        Path newFilePath = downloaded_directory.resolve(fileName);
                        IPeer iPeer = (IPeer) assRegistry.lookup(peerName);
                        //Retreive
                        RetrieveResponse response = iPeer.retrieve(fileName);
                        byte[] bytes = response.content;
                        FileInfo fileInfo = response.fileInfo;
                        fileInfo.setOwned(false);
                        //Save file
                        OutputStream fileOutputStream = new FileOutputStream(newFilePath.toFile());
                        BufferedOutputStream  bufferedOutputStream= new BufferedOutputStream(fileOutputStream);
                        bufferedOutputStream.write(bytes, 0 ,bytes.length );
                        bufferedOutputStream.flush();
                        fileOutputStream.close();
                        bufferedOutputStream.close();
                        this.fileToInfo.put(newFilePath.toFile().getName(),fileInfo);
                        iSuperpeer.registry(this.id,fileName);
                        System.out.println("File "+fileName+" registered successfully for peer "+this.id);
                        //Create thread to check if the file is outdated
                        if(this.isPull){
                            new CheckOutDatedFile(this.id,iPeer,iSuperpeer,fileName,this.fileToInfo.get(fileName).getVersion(),this.fileToInfo,this.fileToInfo.get(fileName).getTTR()).start();
                        }
                        break;
                    case 3:
                        System.out.println("\nEnter the name of the file to refresh:");
                        String fileNameToRefresh = reader.readLine().trim();
                        Registry refRegistry = LocateRegistry.getRegistry(Integer.parseInt(this.fileToInfo.get(fileNameToRefresh).getOriginSuperpeer().split(":")[1]));
                        Path fileToRefresh = downloaded_directory.resolve(fileNameToRefresh);
                        IPeer iPeerRefresh = (IPeer) refRegistry.lookup(this.fileToInfo.get(fileNameToRefresh).getOriginServer());
                        //Retreive
                        RetrieveResponse responseRef = iPeerRefresh.retrieve(fileNameToRefresh);
                        byte[] bytesRef = responseRef.content;
                        FileInfo fileInfoRef = responseRef.fileInfo;
                        fileInfoRef.setOwned(false);
                        //Save file
                        OutputStream fileOutputStreamRef = new FileOutputStream(fileToRefresh.toFile());
                        BufferedOutputStream  bufferedOutputStreamRef= new BufferedOutputStream(fileOutputStreamRef);
                        bufferedOutputStreamRef.write(bytesRef, 0 ,bytesRef.length );
                        bufferedOutputStreamRef.flush();
                        fileOutputStreamRef.close();
                        bufferedOutputStreamRef.close();
                        this.fileToInfo.put(fileToRefresh.toFile().getName(),fileInfoRef);
                        iSuperpeer.registry(this.id,fileNameToRefresh);
                        System.out.println("File "+fileNameToRefresh+" refreshed successfully");
                        //Create thread to check if the file is outdated
                        if(this.isPull){
                            new CheckOutDatedFile(this.id,iPeerRefresh,iSuperpeer,fileNameToRefresh,this.fileToInfo.get(fileNameToRefresh).getVersion(),this.fileToInfo,this.fileToInfo.get(fileNameToRefresh).getTTR()).start();
                        }
                        break;
                    //Exit (previous deregistration of all files)
                    case 4:
                        files = this.owned_directory.toFile().listFiles();
                        if(files !=null){
                            for (File child : files) {
                                boolean success = iSuperpeer.deregister(this.id, child.getName());
                                if(success){
                                    System.out.println("File "+child.getName()+" deregistered successfully for peer "+this.id);
                                }else{
                                    System.out.println("ERROR. File "+child.getName()+" could not be deregistered for peer "+this.id);
                                }
                            }
                        }
                        System.out.println("Peer shut down correctly");
                        System.exit(0);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //Thread used for checking outdated files
    private class CheckOutDatedFile extends Thread{
        private Integer TTR;
        private String fileName;
        private Integer version;
        private IPeer iPeer;
        private ISuperpeer iSuperpeer;
        private String id;
        ConcurrentHashMap<String,FileInfo> fileToInfo;

        public CheckOutDatedFile(String id,IPeer iPeer,ISuperpeer iSuperpeer,String fileName,Integer version, ConcurrentHashMap<String,FileInfo> fileToInfo,Integer TTR){
            this.TTR=TTR;
            this.fileName=fileName;
            this.iPeer=iPeer;
            this.version=version;
            this.fileToInfo=fileToInfo;
            this.iSuperpeer=iSuperpeer;
            this.id=id;
        }
        public void run(){
            try{
                while (true){
                    this.sleep(this.TTR*1000);
                    if(iPeer.isOutdated(this.fileName,this.version)){
                        this.fileToInfo.get(this.fileName).setValid(false);
                        System.out.println("IS OUTDATED - RESPONSE. File name: "+fileName+". Version: "+version+". Response: True");
                        this.iSuperpeer.deregister(this.id,fileName);
                        break;
                    }
                    System.out.println("IS OUTDATED - RESPONSE. File name: "+fileName+". Version: "+version+". Response: False");
                }
            }catch (Exception e){
                System.out.println("Error checking outdated files");
                e.printStackTrace();
            }

        }
    }


}