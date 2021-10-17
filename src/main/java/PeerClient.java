import java.io.*;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;

public class PeerClient extends Thread{
    private String id;
    private String indexer;
    private Path shared_directory;

    public PeerClient(String id, String indexer, Path shared_directory){
        this.indexer =indexer;
        this.id =id;
        this.shared_directory=shared_directory;
    }

    public void run() {
        try{
            String[] addPort = this.indexer.split(":");
            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(addPort[1]));
            IIndexer iIndexer = (IIndexer) registry.lookup(this.indexer);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            Boolean retry = true;
            File[] files = this.shared_directory.toFile().listFiles();
            if(files !=null){
                for (File child : files) {
                    boolean success = iIndexer.registry(this.id, child.getName());
                    if(success){
                        System.out.println("File "+child.getName()+" registered successfully for peer "+this.id);
                    }else{
                        System.out.println("ERROR. File "+child.getName()+" could not be registered for peer "+this.id);
                    }
                }
            }
            //Create another thread to watch directory events
            new Watcher(this.id,iIndexer,this.shared_directory).start();
            int choice = 0;
            while(true){
                retry = true;
                while (retry){
                    try{
                        System.out.println("Choose action");
                        System.out.println("[1] To look for alternatives to download a file");
                        System.out.println("[2] To download a specific file from specific peer");
                        System.out.println("[3] To exit");
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
                        HashSet<String> set = iIndexer.search(name);
                        if(set.size()==0){
                            System.out.println("File is not in the server");
                        }else{
                            System.out.println("You can download "+name+" from the following peers: "+set.toString());
                        }
                        break;
                    //We request to another peer a file
                    case 2:
                        System.out.println("\nEnter the name of the file you are looking for:");
                        String fileName = reader.readLine().trim();
                        System.out.println("\nEnter the peer you want to download from (address:port):");
                        String peerName = reader.readLine().trim();
                        Path newFilePath = shared_directory.resolve(fileName);
                        IPeer iPeer = (IPeer) registry.lookup(peerName);
                        byte[] bytes = iPeer.retrieve(fileName);
                        OutputStream fileOutputStream = new FileOutputStream(newFilePath.toFile());
                        BufferedOutputStream  bufferedOutputStream= new BufferedOutputStream(fileOutputStream);
                        bufferedOutputStream.write(bytes, 0 ,bytes.length );
                        bufferedOutputStream.flush();
                        fileOutputStream.close();
                        bufferedOutputStream.close();
                        break;
                    //Exit
                    case 3:
                        files = this.shared_directory.toFile().listFiles();
                        if(files !=null){
                            for (File child : files) {
                                boolean success = iIndexer.deregister(this.id, child.getName());
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



}