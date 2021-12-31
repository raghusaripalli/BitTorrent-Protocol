import java.nio.ByteBuffer;

public class ApplicationUtil {
    public static int bytesToInt(byte[] buffer){
        return ByteBuffer.wrap(buffer).getInt();
    }

    public static byte[] intToBytes(int val){
        return ByteBuffer.allocate(4).putInt(val).array();
    }
}
