import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Logger {

    private static BufferedWriter bw;
    private static FileWriter fw;

    private Logger(){
        // Do not allow to create objects
    }

    public static void initializeLogger(int peerId){
        try {
            fw = new FileWriter("log_peer_" +peerId + ".log");
            bw = new BufferedWriter(fw);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void terminateLogger(){
        try {
            if( bw != null){
                bw.close();
            }

            if( fw != null){
                fw.close();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static synchronized void logInfo(String log){
        StringBuilder data = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        data.append('[').append(now.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))).append(']');
        data.append(" ").append(log).append("\n");
        try {
            bw.write(data.toString());
            bw.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
