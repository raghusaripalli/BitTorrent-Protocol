import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class PeerApplication {
    public static Peer myself;
    public static BitSet bitSet;
    private ApplicationProperties properties;
    private ServerSocket serverSocket;
    private Thread connectionsListenerThread;
    public static Map<Integer, Peer> peerLookup;
    public static Map<Integer, Socket> peerSocketLookup;
    public static Set<Integer> interestedNeighbors;
    public static Set<Integer> preferredNeighbors;
    public static Map<Integer, Boolean> peerChokeStatusLookup;
    public static int optUnchokedPeerId = 0;
    public static Timer preferredNeighborTimer;
    public static Timer optUnchokeTimer;
    public static BitSet requestedBitSet;
    public static boolean shouldStopExecution;
    private static int totalConnectionsToAccept;

    private static List<Thread> messageListenerThreads;

    public PeerApplication(int peerId){
        myself = new Peer(peerId);
        properties = ApplicationProperties.getInstance();
        messageListenerThreads = Collections.synchronizedList(new ArrayList<>());
        peerLookup = Collections.synchronizedMap(new HashMap<>());
        peerSocketLookup = Collections.synchronizedMap(new HashMap<>());
        interestedNeighbors = Collections.synchronizedSet(new HashSet<>());
        preferredNeighbors = Collections.synchronizedSet(new HashSet<>());
        peerChokeStatusLookup = Collections.synchronizedMap(new HashMap<>());
        requestedBitSet = new BitSet(properties.getTotalPieces());

    }

    public static void addListenerThread(Thread thread){
        messageListenerThreads.add(thread);
    }

    public void run(){
        parsePeerDetails();

        //start listening to the socket
        try{
            this.serverSocket = new ServerSocket(myself.getPort());
            this.connectionsListenerThread = new Thread(new ConnectionsListenerThread(serverSocket, totalConnectionsToAccept));
            this.connectionsListenerThread.start();

        }catch (IOException ioException) {
            ioException.printStackTrace();
        }

        //create new connection with every peer that appears above myself in the PeersInfo.cfg file
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader("PeerInfo.cfg"))){
            String line = null;

            while((line = bufferedReader.readLine()) != null){
                String[] parts = line.split(" ");
                if(Integer.parseInt(parts[0]) == myself.getId()){
                    break;
                }
                Peer peer = this.peerLookup.get(Integer.parseInt(parts[0]));
                Socket socket = new Socket(peer.getAddress(), peer.getPort());
                Logger.logInfo("Peer " + myself.getId() + " makes a connection to Peer "
                        + peer.getId());
                peerSocketLookup.put(peer.getId(), socket);
                peerChokeStatusLookup.put(peer.getId(), true);
                Thread thread = new Thread(new PeerMessageListenerThread(socket, peer, false));
                messageListenerThreads.add(thread);
                thread.start();
            }
        }catch (IOException ioException){
            ioException.printStackTrace();
        }

        preferredNeighborTimer = new Timer();
        int unChokingInterval = Integer.parseInt(properties.get("UnchokingInterval")) * 1000;
        preferredNeighborTimer.schedule(new PreferredNeighborSelectionTimerTask(), unChokingInterval, unChokingInterval);

        optUnchokeTimer = new Timer();
        int optimisticUnchokingInterval = Integer.parseInt(properties.get("OptimisticUnchokingInterval")) * 1000;
        optUnchokeTimer.schedule(new OptimisticUnchokedNeighborTimeTask(), optimisticUnchokingInterval, optimisticUnchokingInterval);

        Thread missingPieceChecker = new Thread(new MissingPieceCheckerThread());
        missingPieceChecker.start();

        try{
            while(!doesAllPeerHaveFile()){
                Thread.sleep(5000);
            }
            for(Thread t : messageListenerThreads){
                if(t.isAlive()){
                    t.join();
                }
            }
            if(missingPieceChecker.isAlive()){
                missingPieceChecker.join();
            }
            this.serverSocket.close();
            preferredNeighborTimer.cancel();
            optUnchokeTimer.cancel();

            Thread.sleep(10000);
        }catch(Exception e){
            Logger.logInfo("Exception occurred while waiting for thread to finish : " + e.getMessage());
        }
        Logger.logInfo("All peers have finished downloading the file. Proceeding to shut down");
    }

    private void parsePeerDetails(){
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader("PeerInfo.cfg"))){
            String line = null;
            totalConnectionsToAccept = 0;
            int increment = 0;
            while((line = bufferedReader.readLine()) != null){
                String[] parts = line.split(" ");
                totalConnectionsToAccept += increment;
                if(Integer.parseInt(parts[0]) == myself.getId()){
                    increment = 1;
                    myself.setPort(Integer.parseInt(parts[2]));
                    myself.setAddress(parts[1]);
                    myself.setHasCompleteFile(Integer.parseInt(parts[3]) == 1);
                    bitSet = new BitSet(ApplicationProperties.getInstance().getTotalPieces());
                    if(myself.getHasCompleteFile()){
                        bitSet.set(0, ApplicationProperties.getInstance().getTotalPieces());
                    }
                }else{
                    Peer peer = new Peer(Integer.parseInt(parts[0]), Integer.parseInt(parts[2])
                            , parts[1], Integer.parseInt(parts[3]) == 1);
                    this.peerLookup.put(peer.getId(), peer);
                }
            }
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    private class PreferredNeighborSelectionTimerTask extends TimerTask{

        @Override
        public void run() {
            synchronized (this){
                int noOfPreferredNeighbors = Integer.parseInt(properties.get("NumberOfPreferredNeighbors"));
                Set<Integer> neighborsToChoke = new HashSet<>(preferredNeighbors);
                preferredNeighbors.clear();

                if(interestedNeighbors.size() > noOfPreferredNeighbors){
                    List<Integer> temp = new ArrayList<>(interestedNeighbors);
                    if(bitSet.cardinality() == properties.getTotalPieces()){ //Do things random
                        Random random = new Random();
                        while(preferredNeighbors.size() != noOfPreferredNeighbors){
                            int randomIdx = random.nextInt(temp.size());
                            preferredNeighbors.add(temp.get(randomIdx));
                            temp.remove(randomIdx);
                        }
                    }else{
                        temp.sort((peer1, peer2) -> {
                            Peer peer1Obj = peerLookup.get(peer1);
                            Peer peer2Obj = peerLookup.get(peer2);
                            return (int) (peer2Obj.bytesDownloadedFrom.get() - peer1Obj.bytesDownloadedFrom.get());
                        });
                        for(int i = 0; i < noOfPreferredNeighbors; i++){
                            preferredNeighbors.add(temp.get(i));
                        }
                    }

                }else{
                    preferredNeighbors.addAll(interestedNeighbors);
                }

                for(Peer peer : peerLookup.values()){
                    peer.bytesDownloadedFrom.set(0);
                }

                //neighbors to unchoke
                Set<Integer> neighborsToUnchoke = new HashSet<>(preferredNeighbors);
                //neighborsToUnchoke.removeAll(neighborsToChoke);

                //all the neighbors for which choke message has to be sent
                neighborsToChoke.removeAll(preferredNeighbors);

                for(Integer peerId : neighborsToUnchoke){
                    Socket socket = peerSocketLookup.get(peerId);
                    if(socket == null){
                        continue;
                    }
                    try{
                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                        PeerMessageListenerThread.writeToSocket(outputStream,
                                Message.createMessage(Message.TYPE_UNCHOKE).toBytes());
                        peerChokeStatusLookup.put(peerId, false);
                    }catch(IOException ioe) {
                        Logger.logInfo("Exception occurred while unchoking Neighbors :" + ioe.getMessage());
                    }
                }

                for(Integer peerId : neighborsToChoke){
                    Socket socket = peerSocketLookup.get(peerId);
                    if(peerId == optUnchokedPeerId || socket == null){
                        continue;
                    }
                    try{
                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                        PeerMessageListenerThread.writeToSocket(outputStream,
                                Message.createMessage(Message.TYPE_CHOKE).toBytes());
                        peerChokeStatusLookup.put(peerId, true);
                    }catch(IOException ioe) {
                        Logger.logInfo("Exception occurred while choking Neighbors :" + ioe.getMessage());
                    }
                }
                Logger.logInfo("Peer " + myself.getId() + " has the interested neighbors " + interestedNeighbors.toString());
                Logger.logInfo("Peer "+ myself.getId() + " has the preferred neighbors " + preferredNeighbors.toString());
            }
        }
    }

    private class OptimisticUnchokedNeighborTimeTask extends TimerTask{

        @Override
        public void run() {
            synchronized (this){
                Set<Integer> tempInterestedPeers = new HashSet<>(interestedNeighbors);
                Set<Integer> tempPrefPeers = new HashSet<>(preferredNeighbors);
                tempInterestedPeers.removeAll(tempPrefPeers);

                if(optUnchokedPeerId != 0 && !preferredNeighbors.contains(optUnchokedPeerId)){
                    Socket skt = peerSocketLookup.get(optUnchokedPeerId);
                    peerChokeStatusLookup.put(optUnchokedPeerId, true);
                    try{
                        if(skt != null){
                            DataOutputStream dos = new DataOutputStream(skt.getOutputStream());
                            PeerMessageListenerThread.writeToSocket(dos,
                                    Message.createMessage(Message.TYPE_CHOKE).toBytes());
                        }
                    }catch(Exception e){
                        Logger.logInfo("Error occurred while choking previous optimistic unchoked neighbor :"
                        + e.getMessage());
                    }
                }
                Logger.logInfo("Peer "+ myself.getId() + " has following options for optimistic unchoking: "
                    + tempInterestedPeers.toString());
                if(!tempInterestedPeers.isEmpty()){
                    Random random = new Random();
                    int idx = random.nextInt(tempInterestedPeers.size());
                    optUnchokedPeerId = (int) tempInterestedPeers.toArray()[idx];

                    peerChokeStatusLookup.put(optUnchokedPeerId, false);

                    Socket skt = peerSocketLookup.get(optUnchokedPeerId);
                    try{
                        if(skt != null){
                            DataOutputStream dos = new DataOutputStream(skt.getOutputStream());
                            PeerMessageListenerThread.writeToSocket(dos,
                                    Message.createMessage(Message.TYPE_UNCHOKE).toBytes());
                        }
                    }catch(Exception e){
                        Logger.logInfo("Error occurred while unchoking previous optimistic unchoked neighbor :"
                                + e.getMessage());
                    }
                    Logger.logInfo("Peer " + myself.getId() + " has optimistically unchoked neighbor "
                        + optUnchokedPeerId);
                }
            }
        }
    }

    public static boolean doesAllPeerHaveFile(){
        int totalNumberOfPieces = ApplicationProperties.getInstance().getTotalPieces();
        for(Peer peer : peerLookup.values()){
            int cardinality = peer.getBitSet().cardinality();

            if(cardinality != totalNumberOfPieces){
                return false;
            }
        }

        return bitSet.cardinality() == totalNumberOfPieces;
    }
}
