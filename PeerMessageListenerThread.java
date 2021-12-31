import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Random;

public class PeerMessageListenerThread implements Runnable{
    private final Socket socket;
    private Peer peer;
    private DataInputStream inStream;
    private DataOutputStream outStream;
    private final boolean didNeighborPeerInitiate;
    private boolean amIChoked;

    public PeerMessageListenerThread(Socket socket, Peer peer, boolean didNeighborPeerInitiate){
        this(socket, didNeighborPeerInitiate);
        this.peer = peer;
    }

    public PeerMessageListenerThread(Socket socket, boolean didNeighborPeerInitiate){
        this.socket = socket;
        this.didNeighborPeerInitiate = didNeighborPeerInitiate;
        try{
            this.inStream = new DataInputStream(socket.getInputStream());
            this.outStream = new DataOutputStream(socket.getOutputStream());
        }catch(IOException ioException) {
            ioException.printStackTrace();
        }
        this.amIChoked = true;
    }

    @Override
    public void run() {
        try{
            if(this.didNeighborPeerInitiate){
                byte[] handshakeBytes = new byte[Handshake.MESSAGE_LENGTH];
                inStream.read(handshakeBytes);
                Handshake handshake = Handshake.createHandShake(handshakeBytes);
                if(handshake == null) {
                    Logger.logInfo("Handshake failed with a peer");
                }
                Logger.logInfo("Peer "+ PeerApplication.myself.getId()
                        + " is connected from Peer "+ handshake.getPeerId());
                this.peer = PeerApplication.peerLookup.get(handshake.getPeerId());
                PeerApplication.peerSocketLookup.put(this.peer.getId(), this.socket);
                PeerApplication.peerChokeStatusLookup.put(this.peer.getId(), true);

                handshake = new Handshake(PeerApplication.myself.getId());
                PeerMessageListenerThread.writeToSocket(outStream, handshake.toBytes());
            }else{
                //Initiate a handshake
                Handshake handshake = new Handshake(PeerApplication.myself.getId());
                PeerMessageListenerThread.writeToSocket(outStream, handshake.toBytes());
                byte[] handshakeBytes = new byte[Handshake.MESSAGE_LENGTH];
                inStream.read(handshakeBytes);
                Handshake receivedHandshake = Handshake.createHandShake(handshakeBytes);
                if(handshake == null){
                    Logger.logInfo("Handshake failed with a peer");
                }
                Logger.logInfo("Handshake completed with " + receivedHandshake.getPeerId());
            }

            if(!PeerApplication.bitSet.isEmpty()){
                Message bitFieldMessage = Message.createMessage(Message.TYPE_BITFIELD, (BitSet)PeerApplication.bitSet.clone());
                PeerMessageListenerThread.writeToSocket(outStream, bitFieldMessage.toBytes());

            }

            while(true){
                int messageLength = inStream.readInt();

                byte[] messageBytes = new byte[messageLength];
                inStream.read(messageBytes);

                Message message = Message.createMessage(messageBytes);
                switch(message.getType()){
                    case Message.TYPE_BITFIELD:{
                        Logger.logInfo("Received BIT field message from " + peer.getId());
                        this.peer.setBitSet(message.getPayload().getBitSet());

                        BitSet myRequiredPieces = (BitSet) PeerApplication.bitSet.clone();
                        myRequiredPieces.xor(this.peer.getBitSet());
                        myRequiredPieces.andNot(PeerApplication.bitSet);

                        if(myRequiredPieces.isEmpty()){
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_NOT_INTERESTED).toBytes());
                        }else{
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_INTERESTED).toBytes());
                        }
                        break;
                    }
                    case Message.TYPE_INTERESTED:{
                        Logger.logInfo("Peer " + PeerApplication.myself.getId() + " received the 'interested'"
                            + " message from "+ peer.getId());
                        PeerApplication.interestedNeighbors.add(peer.getId());
                        break;
                    }
                    case Message.TYPE_NOT_INTERESTED:{
                        Logger.logInfo("Peer "+ PeerApplication.myself.getId() + " received the 'not interested'"
                            + " message from "+ peer.getId());
                        PeerApplication.interestedNeighbors.remove(peer.getId());
                        break;
                    }
                    case Message.TYPE_HAVE:{
                        Payload payload = message.getPayload();
                        Logger.logInfo("Peer "+ PeerApplication.myself.getId() + " received the 'have' message "
                            + "from " + peer.getId() + " for the piece " + payload.getPieceIndex());

                        peer.getBitSet().set(payload.getPieceIndex());

                        BitSet missingPieces = (BitSet) peer.getBitSet().clone();
                        missingPieces.flip(0, ApplicationProperties.getInstance().getTotalPieces());
                        //Logger.logInfo("Peer "+ peer.getId() + " needs " + missingPieces.toString());


                        if(peer.getBitSet().cardinality() == ApplicationProperties.getInstance().getTotalPieces()){
                            Logger.logInfo("Peer " + peer.getId() + " has downloaded the complete file");
                        }

                        BitSet myRequiredPieces = (BitSet) PeerApplication.bitSet.clone();
                        myRequiredPieces.xor(peer.getBitSet());
                        myRequiredPieces.andNot(PeerApplication.bitSet);

                        if(!myRequiredPieces.isEmpty()){
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_INTERESTED).toBytes());
                        }else{
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_NOT_INTERESTED).toBytes());
                        }
                        break;
                    }
                    case Message.TYPE_REQUEST:{
                        Logger.logInfo("Received Request message from " + peer.getId() + " for the peice "+
                                message.getPayload().getPieceIndex());
                        if(PeerApplication.preferredNeighbors.contains(peer.getId()) ||
                            PeerApplication.optUnchokedPeerId == peer.getId()){
                            int pieceIdx = message.getPayload().getPieceIndex();
                            byte[] pieceData = FileUtility.getPieceBytes(pieceIdx);
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_PIECE, pieceIdx, pieceData).toBytes());
                            /*String pieceStr = new String(pieceData);
                            pieceStr = pieceStr.replaceAll("[^A-Za-z0-9]", "");
                            System.out.println("Sent piece " + pieceIdx + " of size " + pieceStr.length() +
                                    " to" + peer.getId());*/
                        }
                        break;
                    }
                    case Message.TYPE_PIECE:{
                        /*String msg = new String(message.getPayload().getPieceBytes(), StandardCharsets.UTF_8);
                        msg = msg.replaceAll("[^A-Za-z0-9]", "");
                        System.out.println("Parsing a piece " + message.getPayload().getPieceIndex() + " of size "
                                + msg.length() + " from " + peer.getId());*/
                        Payload payload = message.getPayload();

                        //System.out.println("writing into file " + peer.getId());
                        FileUtility.writePieceToFile(payload.getPieceIndex(),
                                payload.getPieceBytes());
                        //System.out.println("successfully finished writing" + peer.getId());
                        PeerApplication.bitSet.set(payload.getPieceIndex());
                        peer.bytesDownloadedFrom.addAndGet(payload.getPieceBytes().length);

                        Logger.logInfo("Peer "+ PeerApplication.myself.getId() + " has downloaded the piece "
                                + payload.getPieceIndex() + " from " + peer.getId() + ". Now the number of pieces" +
                                " it has is " + PeerApplication.bitSet.cardinality());

                        Message haveMessage = Message.createMessage(Message.TYPE_HAVE, payload.getPieceIndex());


                        for(Integer peerId : PeerApplication.peerSocketLookup.keySet()){
                            Socket skt = PeerApplication.peerSocketLookup.get(peerId);
                            if(skt == null){
                                continue;
                            }
                            DataOutputStream dos = new DataOutputStream(skt.getOutputStream());
                            try{
                                PeerMessageListenerThread.writeToSocket(dos, haveMessage.toBytes());
                                //check if not interested messages should be sent
                                if(!isAnyPieceNeededFrom(PeerApplication.peerLookup.get(peerId))){
                                    PeerMessageListenerThread.writeToSocket(dos,
                                            Message.createMessage(
                                                    Message.TYPE_NOT_INTERESTED
                                            ).toBytes());
                                }

                            }catch(IOException ioe){
                                Logger.logInfo("Exception while sending have message : "+ ioe.getMessage());
                            }
                        }

                        BitSet myRequiredPieces = (BitSet) PeerApplication.bitSet.clone();
                        myRequiredPieces.xor(peer.getBitSet());
                        myRequiredPieces.andNot(PeerApplication.bitSet);
                        myRequiredPieces.andNot(PeerApplication.requestedBitSet);

                        if(!myRequiredPieces.isEmpty() && !amIChoked){
                            int pieceIdx = getRandomSetBit(myRequiredPieces);
                            PeerApplication.requestedBitSet.set(pieceIdx);
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_REQUEST, pieceIdx).toBytes());
                            //System.out.println("Requested for the piece " + pieceIdx + " from "+ peer.getId());
                        }
                        break;
                    }
                    case Message.TYPE_CHOKE:{
                        Logger.logInfo("Peer "+ PeerApplication.myself.getId() + " is choked by " + peer.getId());

                        amIChoked = true;
                        break;
                    }
                    case Message.TYPE_UNCHOKE:{
                        Logger.logInfo("Peer "+ PeerApplication.myself.getId() + " is unchoked by " + peer.getId());

                        amIChoked = false;

                        BitSet myRequiredPieces = (BitSet) PeerApplication.bitSet.clone();
                        myRequiredPieces.xor(peer.getBitSet());
                        myRequiredPieces.andNot(PeerApplication.bitSet);
                        myRequiredPieces.andNot(PeerApplication.requestedBitSet);

                        if(!myRequiredPieces.isEmpty()){
                            int pieceIdx = getRandomSetBit(myRequiredPieces);
                            PeerApplication.requestedBitSet.set(pieceIdx);
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_REQUEST, pieceIdx).toBytes());
                            //System.out.println("Requested for the piece " + pieceIdx + " from "+ peer.getId());
                        }else{
                            PeerMessageListenerThread.writeToSocket(outStream,
                                    Message.createMessage(Message.TYPE_NOT_INTERESTED).toBytes());
                        }
                        break;
                    }

                }
                if(peer.getBitSet().cardinality() == ApplicationProperties.getInstance().getTotalPieces()
                        && peer.getBitSet().cardinality() == PeerApplication.bitSet.cardinality()){
                    break;
                }
                //Thread.sleep(500);
            }

            //send my bit field
        }catch(Exception ioException){
            Logger.logInfo("Error occurred while processing the message: " + ioException.getMessage());
        }

    }

    public static boolean isAnyPieceNeededFrom(Peer p){
        BitSet myRequiredPieces = (BitSet) PeerApplication.bitSet.clone();
        myRequiredPieces.xor(p.getBitSet());
        myRequiredPieces.andNot(PeerApplication.bitSet);
        myRequiredPieces.andNot(PeerApplication.requestedBitSet);
        return !myRequiredPieces.isEmpty();
    }

    private int getRandomSetBit(BitSet bitset){
        int[] setBits = bitset.stream().toArray();
        Random random = new Random();
        return setBits[random.nextInt(setBits.length)];
    }

    public static synchronized void writeToSocket(DataOutputStream dos, byte[] data) throws IOException {
        dos.write(data);
        dos.flush();
    }
}
