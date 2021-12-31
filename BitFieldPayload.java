import java.util.BitSet;

public class BitFieldPayload implements Payload {

    private BitSet bitset;

    public BitFieldPayload(BitSet bitset){
        this.bitset = bitset;
    }

    public BitFieldPayload(byte[] bytes){
        this.bitset = BitSet.valueOf(bytes);
    }

    @Override
    public byte[] toBytes() {
        return this.bitset.toByteArray();
    }

    @Override
    public BitSet getBitSet(){
        return this.bitset;
    }
}
