public class EmptyPayload implements Payload{

    public EmptyPayload(){
        //no one else can construct the object
    }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }
}
