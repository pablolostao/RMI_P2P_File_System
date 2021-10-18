import java.io.*;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.util.HashSet;


public class PeerServer implements IPeer{
    //Path to shared directory
    private Path shared_directory;

    public PeerServer() throws RemoteException {
        super();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Boolean retry = true;
        //Init shared directory
        while(retry){
            try{
                System.out.println("Insert the path of the shared directory:");
                String dirPath = reader.readLine().trim();
                File dir = new File(dirPath);
                if (dir.isDirectory()){
                    this.shared_directory = dir.toPath();
                    retry = false;
                }else{
                    System.out.println("The path is not a directory");
                }
            }catch (Exception e){
                System.out.println("Path not valid");
            }
        }
    }

    //Get shared directory
    public Path getShared_directory() {
        return shared_directory;
    }

    //Retrieve a file
    public synchronized byte[] retrieve(String fileName) throws RemoteException{
        File internalFile = this.shared_directory.resolve(fileName).toFile();
        byte[] mybytearray  = new byte [(int)internalFile.length()];
        return mybytearray;
    }

    //Receives a queryhit and prints de result
    public synchronized void queryhit(String messageID, String fileName, HashSet<String> set,String superPeer) throws RemoteException{
        if(set.size()==0){
            return;
        }
        System.out.println("QUERYHIT - RECEIVED. Original sender: "+superPeer+" for "+fileName+". Set of peers:");
        System.out.println(set);
    }

}
