
public class peerProcess {
    public static int peerId = 0;

    public static void main(String[] args){
        peerId = Integer.parseInt(args[0]);
        Logger.initializeLogger(peerId);
        PeerApplication application = new PeerApplication(peerId);
        application.run();
        Logger.terminateLogger();
    }
}
