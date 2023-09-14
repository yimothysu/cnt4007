import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

public class PeerInfoCfgReader {
    public static Vector<RemotePeerInfo> read()
    {
        String st;
        int i1;
        Vector<RemotePeerInfo> peerInfoVector = new Vector<RemotePeerInfo>();
        try {
            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");
                //System.out.println("tokens begin ----");
                //for (int x=0; x<tokens.length; x++) {
                //    System.out.println(tokens[x]);
                //}
                //System.out.println("tokens end ----");

                peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));

            }

            in.close();
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }

        return peerInfoVector;
    }
}
