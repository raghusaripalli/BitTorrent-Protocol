import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Handshake {
    private static final String HEADER = "P2PFILESHARINGPROJ";
    private static final int HEADER_LENGTH = 18;
    private static final int PEER_ID_LENGTH = 4;
    private static final int ZEROBITS_LENGTH = 10;
    public static final int MESSAGE_LENGTH = 32;

    private byte[] headerArr;
    private byte[] zeroBitsArr;
    private byte[] peerIdArr;

    private Handshake(byte[] headerArr, byte[] zeroBitsArr, byte[] peerIdArr){
        this.headerArr = headerArr;
        this.zeroBitsArr = zeroBitsArr;
        this.peerIdArr = peerIdArr;
    }

    public Handshake(int peerId){
        this.headerArr = HEADER.getBytes(StandardCharsets.UTF_8);
        this.peerIdArr = String.valueOf(peerId).getBytes(StandardCharsets.UTF_8);
        this.zeroBitsArr = "0000000000".getBytes(StandardCharsets.UTF_8);
    }

    public static Handshake createHandShake(byte[] handShakeByteArr){
        byte[] headerArr = Arrays.copyOfRange(handShakeByteArr,0, HEADER_LENGTH);
        byte[] zeroBitsArr = Arrays.copyOfRange(handShakeByteArr, HEADER_LENGTH, HEADER_LENGTH + ZEROBITS_LENGTH);
        byte[] peerIdArr = Arrays.copyOfRange(handShakeByteArr, HEADER_LENGTH + ZEROBITS_LENGTH, MESSAGE_LENGTH);

        if(HEADER.equals(new String(headerArr, StandardCharsets.UTF_8))){
            return new Handshake(headerArr, zeroBitsArr, peerIdArr);
        }

        return null;
    }

    public byte[] toBytes(){
        byte[] handShakeByteArr = new byte[MESSAGE_LENGTH];
        System.arraycopy(this.headerArr,0,handShakeByteArr,0, HEADER_LENGTH);
        System.arraycopy(this.zeroBitsArr,0,handShakeByteArr, HEADER_LENGTH, ZEROBITS_LENGTH);
        System.arraycopy(this.peerIdArr,0,handShakeByteArr,HEADER_LENGTH + ZEROBITS_LENGTH, PEER_ID_LENGTH);
        return handShakeByteArr;
    }

    public int getPeerId(){
        return Integer.parseInt(new String(this.peerIdArr));
    }
}
