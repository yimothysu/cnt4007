import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class PeerInfoCfgReader {
    public static LinkedHashMap<String, RemotePeerInfo> read()
    {
        String st;
        int i1;
        LinkedHashMap<String, RemotePeerInfo> peerInfoMap = new LinkedHashMap<String, RemotePeerInfo>();
        try {
            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");
                //System.out.println("tokens begin ----");
                //for (int x=0; x<tokens.length; x++) {
                //    System.out.println(tokens[x]);
                //}
                //System.out.println("tokens end ----");

                String peerId = tokens[0];
                peerInfoMap.put(peerId, new RemotePeerInfo(peerId, tokens[1], tokens[2]));

            }

            in.close();
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }

        return peerInfoMap;
    }

}
