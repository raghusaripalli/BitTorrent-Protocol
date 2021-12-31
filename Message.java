import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class Message {
    private static final int LENGTH = 4;
    private static final int TYPE_LENGTH = 1;

    public static final int TYPE_CHOKE = 0;
    public static final int TYPE_UNCHOKE = 1;
    public static final int TYPE_INTERESTED = 2;
    public static final int TYPE_NOT_INTERESTED = 3;
    public static final int TYPE_HAVE = 4;
    public static final int TYPE_BITFIELD = 5;
    public static final int TYPE_REQUEST = 6;
    public static final int TYPE_PIECE = 7;


    private int length;
    private int type;
    private Payload payload;


    private Message(){
        //do nothing
    }

    private Message(int length, int type, Payload payload){
        this.length = length;
        this.type = type;
        this.payload = payload;
    }

    public byte[] toBytes(){
        byte[] payloadBytes = this.payload.toBytes();
        byte[] output = new byte[payloadBytes.length +  TYPE_LENGTH + LENGTH];
        byte[] typeBuffer = new byte[]{(byte)this.type};
        byte[] messageLengthBuffer = ApplicationUtil.intToBytes(typeBuffer.length + payloadBytes.length);

        System.arraycopy(messageLengthBuffer, 0, output, 0, LENGTH);
        System.arraycopy(typeBuffer, 0, output, LENGTH, TYPE_LENGTH);
        System.arraycopy(payloadBytes, 0, output, LENGTH + TYPE_LENGTH, payloadBytes.length);

        return output;
    }

    public static Message createMessage(byte[] bytes){
        int type = bytes[0];

        byte[] payloadBytes = new byte[bytes.length - TYPE_LENGTH];
        System.arraycopy(bytes, TYPE_LENGTH, payloadBytes, 0, payloadBytes.length);

        switch(type){
            case TYPE_CHOKE:
            case TYPE_INTERESTED:
            case TYPE_NOT_INTERESTED:
            case TYPE_UNCHOKE: return new Message(bytes.length, type, new EmptyPayload());
            case TYPE_HAVE:
            case TYPE_REQUEST: return new Message(bytes.length, type, new PieceIndexPayload(payloadBytes));
            case TYPE_BITFIELD: return new Message(bytes.length, type, new BitFieldPayload(payloadBytes));
            default: return new Message(bytes.length, type, new PiecePayload(payloadBytes));
        }
    }

    public static Message createMessage(int type){
        return new Message(TYPE_LENGTH, type, new EmptyPayload());
    }

    public static Message createMessage(int type, int pieceIndex){
        return new Message(TYPE_LENGTH, type, new PieceIndexPayload(pieceIndex));
    }

    public static Message createMessage(int type, int pieceIdx, byte[] bytes){
        return new Message(TYPE_LENGTH, type, new PiecePayload(pieceIdx, bytes));
    }

    public static Message createMessage(int type, BitSet bitset){
        return new Message(TYPE_LENGTH, type, new BitFieldPayload(bitset));
    }

    public int getType(){
        return this.type;
    }

    public Payload getPayload(){
        return this.payload;
    }

}
