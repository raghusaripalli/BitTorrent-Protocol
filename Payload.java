import java.util.BitSet;

public interface Payload {
    int PIECE_IDX_LENGTH = 4;

    byte[] toBytes();

    default int getPieceIndex(){
        return 0;
    }

    default byte[] getPieceBytes(){
        return new byte[0];
    }

    default BitSet getBitSet(){
        return new BitSet();
    }
}
