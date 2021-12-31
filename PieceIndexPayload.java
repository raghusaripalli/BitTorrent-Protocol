public class PieceIndexPayload implements Payload{
    int pieceIdx;

    public PieceIndexPayload(int pieceIdx){
        this.pieceIdx = pieceIdx;
    }

    public PieceIndexPayload(byte[] bytes){
        this.pieceIdx = ApplicationUtil.bytesToInt(bytes);
    }

    @Override
    public byte[] toBytes() {
        return ApplicationUtil.intToBytes(this.pieceIdx);
    }

    @Override
    public int getPieceIndex() {
        return this.pieceIdx;
    }
}
