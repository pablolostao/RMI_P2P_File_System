import java.io.File;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.*;

public class Watcher extends Thread{
    ISuperpeer iSuperpeer = null;
    String id = null;
    String superpeer = null;
    Path shared_directory = null;
    WatchService watcher = null;
    Integer nextMessageID = null;
    Boolean isPull;
    ConcurrentHashMap<String,FileInfo> fileToInfo;

    public Watcher(String id, ISuperpeer iSuperpeer, Path shared_directory,Integer nextMessageID,String superpeer,ConcurrentHashMap<String,FileInfo> fileToInfo,Boolean isPull){
        this.id = id;
        this.iSuperpeer = iSuperpeer;
        this.shared_directory=shared_directory;
        this.nextMessageID=nextMessageID;
        this.superpeer=superpeer;
        this.fileToInfo=fileToInfo;
        this.isPull=isPull;
    }

    //Method to look for events and act as needed
    void processEvents() throws Exception{
        while(true) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                System.out.println("Exception");
                return;
            }
            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                if (kind == OVERFLOW) {
                    continue;
                }
                if(kind == ENTRY_MODIFY){
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filepath = ev.context();
                    File file = new File(filepath.toString());
                    this.fileToInfo.get(file.getName()).setVersion(this.fileToInfo.get(file.getName()).getVersion()+1);
                    if(!this.isPull){
                        iSuperpeer.invalidation(this.superpeer+"-"+this.id+"-"+nextMessageID.toString()+"-(I)",4,file.getName(),this.fileToInfo.get(file.getName()).getVersion(),this.id);
                        this.nextMessageID++;
                        System.out.println("INVALIDATION SENT to superpeer for "+file.getName()+". New version: "+this.fileToInfo.get(file.getName()).getVersion().toString());
                    }else{
                        System.out.println("INVALIDATION - no action because pull approach");
                    }
                    continue;
                }
                if (kind == ENTRY_CREATE) {
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filepath = ev.context();
                    File file = new File(filepath.toString());
                    boolean success = iSuperpeer.registry(this.id, file.getName());
                    if(success){
                        System.out.println("File "+file.getName()+" registered successfully for peer "+this.id);
                    }else{
                        System.out.println("ERROR. File "+file.getName()+" could not be registered for peer "+this.id);
                    }
                    continue;
                }
                if (kind == ENTRY_DELETE) {
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filepath = ev.context();
                    File file = new File(filepath.toString());
                    boolean success = iSuperpeer.deregister(this.id, file.getName());
                    if(success){
                        System.out.println("File "+file.getName()+" deregistered successfully for peer "+this.id);
                    }else{
                        System.out.println("ERROR. File "+file.getName()+" could not be deregistered for peer "+this.id);
                    }
                    continue;
                }
            }
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }
    public void run() {
        try{
            this.watcher = FileSystems.getDefault().newWatchService();
            shared_directory.register(this.watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            processEvents();
        }catch (Exception e){
            System.err.println(e);
        }
    }
}