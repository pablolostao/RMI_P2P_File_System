# RMI_P2P_File_System
This repository implements a peer to peer file system in Java, and the main difference with https://github.com/pablolostao/peertopeersystem is that this one uses RMI insted of
pure sockets, so it is much easier to follow execution and to read the code. It also implements new functionalities and now we do not have many peers and one indexer, in this
case we have a network of superpeers that have many peers "behind" them.

It can be run both locally and over a network. The operation is simple, each time a peer is created it indicates which folder it wants to share, indicating the absolute path
of this folder in its local file system. Automatically the system will register in the corresponding superpeer all the files (including subfolders) stored in the shared folder.
A WatcherService is provided, which will automatically notify the superpeer of all changes (deletion, modification and creation) that occur in that folder. When a peer sends a
request to its superpeer for a specific file, the superpeer replies with the peers behind the same superpeer that own the file and broadcasts the request among its neighbors.
When a superpeer receives a message that it has already received or TTL is equals to 0, the superpeer stops the broadcast. Every superpeer will reply with the peers behind it
that own the file, and the reply will go through the same path that the request but from the end to the beginning.

Peers can:

1. Look for a file. At this moment, only file name is used for matching.
2. Retrieve a file. They choose one peer of the list obtained in step 1. and it will download the file from the owner of the file and storing it in the shared folder (and the superpeer will be notified)
3. Exit

# Prerequisites:
You must have JDK installed to compile this project and JRE to run it (tested with openjdk version "1.8.0_292")

Git

You must have Maven (tested with Apache Maven 3.6.0) (If not "sudo apt install maven")

# Execution
Each instance of the program is either a superpeer or a peer. The network status if fixed and must be defined in a config.json as in src/ folder.
The program has two arguments, config.json file and ID of the node (peer or superpeer)

1.  git clone https://github.com/pablolostao/RMI_P2P_File_System/
2.  cd RMI_P2P_File_System/
2.  mvn package
3.  java -cp target/GnutellaP2PFileSystem-1.0-SNAPSHOT.jar Main "jsonfile" "nodeId"

**Json file must be in target/classes
