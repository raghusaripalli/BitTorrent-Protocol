import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Peer {
    private int id;
    private int port;
    private String address;
    private boolean hasCompleteFile;
    private BitSet bitSet;
    public AtomicLong bytesDownloadedFrom;

    public Peer(int id){
        this.id = id;
    }
    public Peer(int id, int port, String address, boolean hasCompleteFile){
        this.id = id;
        this.port = port;
        this.address = address;
        this.hasCompleteFile = hasCompleteFile;
        int totalPieces = ApplicationProperties.getInstance().getTotalPieces();
        this.bitSet = new BitSet(totalPieces);
        if(hasCompleteFile){
            this.bitSet.set(0, totalPieces);
        }
        this.bytesDownloadedFrom = new AtomicLong(0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setHasCompleteFile(boolean hasCompleteFile){
        this.hasCompleteFile = hasCompleteFile;
    }

    public boolean getHasCompleteFile(){
        return this.hasCompleteFile;
    }

    public String toString(){
        return this.id + ":" + this.address + ":" + this.port + ":" + this.hasCompleteFile;
    }

    public BitSet getBitSet(){
        return this.bitSet;
    }

    public void setBitSet(BitSet bitSet){
        this.bitSet = bitSet;
    }
}
