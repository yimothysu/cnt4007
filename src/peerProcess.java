//Then the peer process starts and reads the file
//Common.cfg to set the corresponding
//variables. The peer process also reads the file PeerInfo.cfg . It will find that the [has file
//or not] fie ld is 1, which means it has the complete file, it sets all the bits of its bitfield to
//be 1. (On the other hand, if the [has file or not] field is 0, it sets all the bits of its bitfield
//to 0.) Here the bitfield is a data structure where your peer process manages the pieces.
//You have the freedom in how to implement it. This peer also finds out that it is the first
//peer; it will just listen on the port 6008 as specified in the file. Being the first peer, there
//are no other peer s to make connection s to.

import java.io.*;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class peerProcess {
    static String myPeerId;
    static BitField bitfield;
    static HashMap<String, RemotePeerInfo> peerInfoMap;
    static ArrayList<String> precedingPeerIds = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java peerProcess [peerId]");
            return;
        }
        myPeerId = args[0];

        Common common = CommonCfgReader.read();
        peerInfoMap = PeerInfoCfgReader.read();
        readBitFieldFromPeerInfo(common);

        System.out.println(bitfield.bits);

        System.out.println(precedingPeerIds);
        for (String peerId : precedingPeerIds) {
            setUpConnection(peerId);
        }

        System.out.println("Reached");

        listenForConnections();
    }

    /**
     * Initialize client sockets
     * @throws IOException
     */
    private static void setUpConnection(String peerId) throws IOException {
        int sPort = Integer.parseInt(peerInfoMap.get(peerId).peerPort);
        Socket requestSocket = new Socket(peerInfoMap.get(myPeerId).peerAddress, sPort);
        new Handler(requestSocket, myPeerId, peerId, bitfield).start();
    }

    /**
     * Initialize server sockets
     * @throws IOException
     */
    private static void listenForConnections() throws IOException {
        int sPort = Integer.parseInt(peerInfoMap.get(myPeerId).peerPort);
        try (ServerSocket listener = new ServerSocket(sPort)) {
            while (true) {
                Socket clientSocket = listener.accept();
                new Handler(clientSocket, myPeerId, "UNIDENTIFIED CLIENT", bitfield).start();
            }
        }
    }

    private static void readBitFieldFromPeerInfo(Common common) throws IOException {
        String st;
        boolean found = false;
        BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
        while((st = in.readLine()) != null) {

            String[] tokens = st.split("\\s+");

            String rowPeerId = tokens[0];
            if (rowPeerId.equals(myPeerId)) {
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
            precedingPeerIds.add(rowPeerId);
        }
        if (!found) {
            System.out.println("Error: Peer ID " + myPeerId + " not found!");
        }
    }
}
