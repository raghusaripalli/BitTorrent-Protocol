import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Singleton class to store application level properties
 */
public class ApplicationProperties {
    private static ApplicationProperties instance;
    private Properties property;
    private int noOfPieces;

    private ApplicationProperties(){
        this.property = new Properties();
        try(FileReader fileReader = new FileReader("Common.cfg");
            BufferedReader bufferedReader = new BufferedReader(fileReader)){
            String line = null;

            while((line = bufferedReader.readLine()) != null){
                String[] parts = line.split(" ");
                property.setProperty(parts[0], parts[1]);
            }
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
        int pieceSize = Integer.parseInt(this.property.getProperty("PieceSize", "0"));
        int fileSize = Integer.parseInt(this.property.getProperty("FileSize", "0"));
        this.noOfPieces = (int)Math.ceil(fileSize * 1.0 / pieceSize);
    }

    public static ApplicationProperties getInstance(){
        if(instance == null){
            instance = new ApplicationProperties();
        }
        return instance;
    }

    public String get(String key){
        return property.getProperty(key, "");
    }

    public int getTotalPieces(){
        return this.noOfPieces;
    }
}
