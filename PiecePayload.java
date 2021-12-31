public class PiecePayload implements Payload{
    private int pieceIdx;
    private byte[] bytes;

    public PiecePayload(int pieceIdx, byte[] bytes){
        this.pieceIdx = pieceIdx;
        this.bytes = bytes;
    }

    public PiecePayload(byte[] bytes){
        byte[] pieceIdxBytes = new byte[PIECE_IDX_LENGTH];
        byte[] pieceBytes = new byte[bytes.length - PIECE_IDX_LENGTH];
        System.arraycopy(bytes, 0, pieceIdxBytes, 0, PIECE_IDX_LENGTH);
        System.arraycopy(bytes, PIECE_IDX_LENGTH, pieceBytes, 0, pieceBytes.length);
        this.pieceIdx = ApplicationUtil.bytesToInt(pieceIdxBytes);
        this.bytes = pieceBytes;
    }

    @Override
    public byte[] toBytes() {
        byte[] output = new byte[PIECE_IDX_LENGTH + this.bytes.length];
        System.arraycopy(ApplicationUtil.intToBytes(this.pieceIdx), 0, output, 0, PIECE_IDX_LENGTH);
        System.arraycopy(this.bytes, 0, output, PIECE_IDX_LENGTH, this.bytes.length);
        return output;
    }

    @Override
    public int getPieceIndex() {
        return this.pieceIdx;
    }

    @Override
    public byte[] getPieceBytes() {
        return this.bytes;
    }
}
