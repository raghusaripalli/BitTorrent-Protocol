import java.io.DataOutputStream;
import java.net.Socket;
import java.util.BitSet;

public class MissingPieceCheckerThread implements Runnable{

    @Override
    public void run(){
        while(PeerApplication.bitSet.cardinality() != ApplicationProperties.getInstance().getTotalPieces()){
            try{
                Thread.sleep(10000);
                BitSet myRequests = (BitSet) PeerApplication.requestedBitSet.clone();
                BitSet myPieces = (BitSet) PeerApplication.bitSet.clone();
                myRequests.andNot(myPieces);
                if(!myRequests.isEmpty()){
                    PeerApplication.requestedBitSet = (BitSet) PeerApplication.bitSet.clone();

                    for(int peerId : PeerApplication.peerSocketLookup.keySet()){
                        Peer peer = PeerApplication.peerLookup.get(peerId);
                        if(PeerMessageListenerThread.isAnyPieceNeededFrom(peer)){
                            Socket socket = PeerApplication.peerSocketLookup.get(peerId);
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                            PeerMessageListenerThread.writeToSocket(dos,
                                    Message.createMessage(Message.TYPE_INTERESTED).toBytes() );
                        }
                    }
                }

            }catch(Exception e){
                Logger.logInfo("Error occurred while checking for missing pieces :" + e.getMessage());
            }

        }
        Logger.logInfo("Peer " + PeerApplication.myself.getId() + " has downloaded the complete file");
    }
}
