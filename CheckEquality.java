import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CheckEquality {

    public static boolean compareFiles(String filename1, String filename2){
        try(BufferedReader br1 = new BufferedReader(new FileReader(filename1));
            BufferedReader br2 = new BufferedReader(new FileReader(filename2))){
            String s1 = br1.readLine();
            String s2 = br2.readLine();

            while(s1 != null && s2 != null){
                if(!s1.equals(s2)){
                    return false;
                }
                s1 = br1.readLine();
                s2 = br2.readLine();
            }

            return s1 == null && s2 == null;

        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        return false;
    }

    public static void main(String[] args){
        boolean areAllFilesEqual = true;
        try(BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"))){
            int count = 1;

            String line = br.readLine();
            String peer1 = line.split(" ")[0];
            String filename1 = "peer_" + peer1 + "/thefile";

            while((line = br.readLine()) != null){
                String filename2 = "peer_" + line.split(" ")[0] + "/thefile";
                if(!compareFiles(filename1, filename2)){
                    areAllFilesEqual = false;
                    break;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        System.out.println(areAllFilesEqual);
    }
}
