import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileUtility {
    private static ApplicationProperties properties;
    private static int pieceSize;
    private static int fileSize;
    private static String filePath;
    public static int peerId;

    static{
        properties = ApplicationProperties.getInstance();
        pieceSize = Integer.parseInt(properties.get("PieceSize"));
        fileSize = Integer.parseInt(properties.get("FileSize"));
        peerId = peerProcess.peerId;
        //System.out.println("Printing P2PMain id " + PeerProcess.peerId);
        filePath = ".\\peer_" + peerId + "\\" + properties.get("FileName");
        createEmptyFile();
    }

    public static synchronized void createEmptyFile(){
        File file = new File("peer_" + peerId);
        if(file.exists()){
            filePath = file.getPath() + "/" + properties.get("FileName");
            return;
        }
        file.mkdir();
        filePath = file.getPath() + "/" + properties.get("FileName");
        try(OutputStream stream = new FileOutputStream(filePath)){
            byte data = 0;
            int fileSize = Integer.parseInt(properties.get("FileSize"));
            for(int i = 0; i < fileSize; i++) {
                stream.write(data);
            }
        }catch(Exception e){
            Logger.logInfo("exception occurred while creating new file: " + e.getMessage());
        }
    }

    public static synchronized byte[] getPieceBytes(int pieceIndex){
        int pieceLength = Math.min(fileSize - pieceIndex * pieceSize, pieceSize);
        byte[] piece = new byte[pieceLength];
        try(RandomAccessFile file = new RandomAccessFile(filePath,"rw")){
            FileChannel channel = file.getChannel();
            int lockStartIndex = pieceIndex * pieceSize;
            int lockEndIndex = pieceIndex * pieceSize + pieceLength;
            channel.lock(lockStartIndex,lockEndIndex,false);
            ByteBuffer buff = ByteBuffer.allocate(pieceLength);
            channel.read(buff,pieceIndex * pieceSize);
            buff.rewind();
            buff.get(piece);
            //System.out.println("piece : "+new String(piece, StandardCharsets.UTF_8));
            buff.clear();
            channel.close();
        }catch(Exception ex){
            Logger.logInfo("exception occurred while retrieve piece(" + pieceIndex + ") from the file : " +
                    ex.getMessage());
            ex.printStackTrace();
        }
        return piece;
    }

    public static synchronized void writePieceToFile(int pieceIndex, byte[] data){
        //int pieceLength = Math.min(fileSize - pieceIndex * pieceSize, pieceSize);
        try(RandomAccessFile file = new RandomAccessFile(filePath, "rw")){
            FileChannel channel = file.getChannel();
            ByteBuffer buff = ByteBuffer.wrap(data);
            int lockStartIndex = pieceIndex * pieceSize;
            int lockEndIndex = pieceIndex * pieceSize + data.length;
            FileLock lock = channel.lock(lockStartIndex, lockEndIndex,false);
            channel.write(buff, (long) pieceIndex * pieceSize);
            buff.clear();
            channel.close();

        }catch(Exception e){
            Logger.logInfo("Exception occurred while writing piece("+ pieceIndex + ") to file : "
            + e.getMessage());
        }
    }

}
