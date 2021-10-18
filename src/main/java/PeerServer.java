import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.*;

public class PeerServer implements IPeer{
    private Path shared_directory;

    public PeerServer() throws RemoteException {
        super();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Boolean retry = true;
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

    public Path getShared_directory() {
        return shared_directory;
    }

    public synchronized byte[] retrieve(String fileName) throws RemoteException{
        File internalFile = this.shared_directory.resolve(fileName).toFile();
        byte[] mybytearray  = new byte [(int)internalFile.length()];
        return mybytearray;
    }

//    public synchronized boolean queryhit(String messageID, String fileName, HashSet<String> set,String superPeer) throws RemoteException{
//        System.out.println(fileName+" can be found in the following peers: ");
//        System.out.println(set);
//        return true;
//    }

}
