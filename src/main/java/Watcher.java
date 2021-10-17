import java.io.File;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class Watcher extends Thread{
    IIndexer iIndexer = null;
    String id = null;
    Path shared_directory = null;
    WatchService watcher = null;

    public Watcher(String id,IIndexer iIndexer,Path shared_directory){
        this.id = id;
        this.iIndexer=iIndexer;
        this.shared_directory=shared_directory;
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
                if (kind == OVERFLOW || kind == ENTRY_MODIFY) {
                    continue;
                }
                if (kind == ENTRY_CREATE) {
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filepath = ev.context();
                    File file = new File(filepath.toString());
                    boolean success = iIndexer.registry(this.id, file.getName());
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
                    boolean success = iIndexer.deregister(this.id, file.getName());
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