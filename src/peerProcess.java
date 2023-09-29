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
import java.net.ServerSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class peerProcess {
    static String peerId;
    static BitField bitfield;
    static HashMap<String, RemotePeerInfo> peerInfoMap;
    static int index;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java peerProcess [peerId]");
            return;
        }
        peerId = args[0];

        Common common = CommonCfgReader.read();
        peerInfoMap = PeerInfoCfgReader.read();
        readBitFieldFromPeerInfo(common);

        System.out.println(bitfield.bits);

//        for (int i = 0; i < index; i++) {
//            establishTcpConnection(i);
//        }

        System.out.println("Reached");

        listenForConnections();
    }

    private static void listenForConnections() throws IOException {
        int sPort = Integer.parseInt(peerInfoMap.get(peerId).peerPort);
        ServerSocket listener = new ServerSocket(sPort);
        int count = 0;
        while (true) {
            new Handler(listener.accept(), "unknown").start();
            count++;
        }
    }

    private static void readBitFieldFromPeerInfo(Common common) throws IOException {
        String st;
        boolean found = false;
        BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
        int i = 0;
        while((st = in.readLine()) != null) {

            String[] tokens = st.split("\\s+");

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
                index = i;
                break;
            }
            i += 1;
        }
        if (!found) {
            System.out.println("Error: Peer ID " + peerId + " not found!");
        }
    }

    private static void establishRequestConnection(String peerId) throws IOException {
        RemotePeerInfo peerInfo = peerInfoMap.get(peerId);
        int port = Integer.parseInt(peerInfo.peerPort);
        int backlog = 99999999; // Maximum queue length
        InetAddress addr = InetAddress.getByName(peerInfo.peerAddress);

        ServerSocket listener = new ServerSocket(port, backlog, addr);
        new Handler(listener.accept(), peerId).start();
    }
}
