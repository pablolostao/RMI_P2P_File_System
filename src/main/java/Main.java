import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Main {
    public static void main(String args[]) {
        //Check config file
        if (args.length!=2 || (!args[0].equals("config1.json") && !args[0].equals("config2.json"))){
            System.out.println("ERROR. You can only call this program with two argument. The first one must be 'config1.json' or 'config2.json'.");
            System.exit(0);
        }
        //Read object from json
        String jsonString = readFile(args[0]);
        JSONParser parser = new JSONParser();
        JSONObject obj;
        try {
            obj = (JSONObject)parser.parse(jsonString);
            boolean isPull = (boolean) obj.get("isPull");
            obj = (JSONObject)obj.get(args[1]);
            if(obj==null){
                System.out.println("ERROR. Second argument is not valid or error reading config file. Second argument must be one of the IDs described in the config file.");
                System.exit(0);
            }
            String type = (String) obj.get("type");
            //If it is a peer, initialize a peer with its superpeer
            if(type.equals("peer")) {
                try {
                    String id = args[1];
                    ConcurrentHashMap<String,FileInfo> fileToInfo = new ConcurrentHashMap<String,FileInfo>();
                    String superpeer = (String) obj.get("superpeer");
                    Integer TTR = ((Long) obj.get("TTR")).intValue();
                    IPeer iPeer = new PeerServer(id,superpeer,fileToInfo,TTR,isPull);
                    IPeer stub = (IPeer) UnicastRemoteObject.exportObject(iPeer, Integer.parseInt(id.split(":")[1]));
                    Registry registry = LocateRegistry.getRegistry(Integer.parseInt(superpeer.split(":")[1]));
                    registry.rebind(id,stub);
                    System.out.println("Peer initialized at "+id);
                    new PeerClient(id,superpeer,iPeer.getOwned_directory(),iPeer.getDownloaded_directory(),fileToInfo,TTR,isPull).start();
                } catch (Exception e) {
                    System.err.println("Peer exception:");
                    e.printStackTrace();
                }
                //If it is a peer, initialize a superpeer with its neighbors
            }else if(type.equals("superpeer")){
                try {
                    String id = args[1];
                    JSONArray jsonArray = (JSONArray)obj.get("neighbors");
                    String[] neighbors = new String[jsonArray.size()];
                    for (int i = 0; i < jsonArray.size(); i++) {
                        neighbors[i] = (String) jsonArray.get(i);
                    }
                    ISuperpeer iSuperpeer = new Superpeer(id,neighbors);
                    ISuperpeer stub = (ISuperpeer) UnicastRemoteObject.exportObject(iSuperpeer, Integer.parseInt(id.split(":")[1]));
                    Registry registry = LocateRegistry.createRegistry(Integer.parseInt(id.split(":")[1]));
                    registry.rebind(id,stub);
                    System.out.println("Superpeer listening at "+id);
                } catch (Exception e) {
                    System.err.println("Indexer exception:");
                    e.printStackTrace();
                }
            }else{
                System.out.println("ERROR. Only 'peer' and 'superpeer' types admitted.");
                System.exit(0);
            }
        } catch(ParseException e) {
            e.printStackTrace();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    //Auxiliar method to read config.json
    private static String readFile(String file){
        try{
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream(file);
            String text = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            is.close();
            return  text;
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

}
