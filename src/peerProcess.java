//Then the peer process starts and reads the file
//Common.cfg to set the corresponding
//variables. The peer process also reads the file PeerInfo.cfg . It will find that the [has file
//or not] fie ld is 1, which means it has the complete file, it sets all the bits of its bitfield to
//be 1. (On the other hand, if the [has file or not] field is 0, it sets all the bits of its bitfield
//to 0.) Here the bitfield is a data structure where your peer process manages the pieces.
//You have the freedom in how to implement it. This peer also finds out that it is the first
//peer; it will just listen on the port 6008 as specified in the file. Being the first peer, there
//are no other peer s to make connection s to.

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class peerProcess {
    static String peerId;
    static BitField bitfield;
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java peerProcess [peerId]");
            return;
        }
        peerId = args[0];

        Common common = CommonCfgReader.read();

        String st;
        boolean found = false;
        BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
        while((st = in.readLine()) != null) {

            String[] tokens = st.split("\\s+");
            //System.out.println("tokens begin ----");
            //for (int x=0; x<tokens.length; x++) {
            //    System.out.println(tokens[x]);
            //}
            //System.out.println("tokens end ----");

            String rowPeerId = tokens[0];
            if (rowPeerId.equals(peerId)) {
                found = true;

                int bitfieldSize = Math.ceilDiv(common.fileSizeInBytes(), common.pieceSizeInBytes());
                String hasFileField = tokens[3]; // "1" if present, "0" if not
                if (hasFileField.equals("1")) {
                    bitfield = BitField.ones(bitfieldSize);
                } else if (hasFileField.equals("0")) {
                    bitfield = BitField.zeros(bitfieldSize);
                } else {
                    System.out.println("Error: Corrupted Common.cfg file: last column should be 0 or 1");
                }
                break;
            }
        }
        if (!found) {
            System.out.println("Error: Peer ID " + peerId + " not found!");
            return;
        }

        System.out.println(bitfield.bits);
    }
}
