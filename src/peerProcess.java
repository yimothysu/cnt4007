import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The peerProcess class is the entry point for each peer in the network.
 * It is responsible for establishing connections and handling data transfers.
 */
public class peerProcess {

    static String myPeerId;
    static BitField bitfield;
    static HashMap<String, RemotePeerInfo> peerInfoMap;
    static PeerData peerData;

    // An array list to store the IDs of peers that precede this peer
    static ArrayList<String> precedingPeerIds = new ArrayList<>();

    /**
     * The main method initializes connections and listens for incoming connections.
     *
     * @param args Command line arguments; expects the peer ID as the first argument.
     * @throws IOException If an I/O error occurs.
     */
    public static void main(String[] args) throws IOException {

        // Validate that the peer ID is provided as a command line argument
        if (args.length == 0) {
            System.out.println("Usage: java peerProcess [peerId]");
            return;
        }

        // Initialize variables based on the command line arguments and configuration files
        myPeerId = args[0];
        Common common = CommonCfgReader.read();
        peerInfoMap = PeerInfoCfgReader.read();
        readBitFieldFromPeerInfo(common);

        System.out.println(bitfield.bits);

        peerData = new PeerData();

        // Establish connections with peers that precede this one in the network
        System.out.println(precedingPeerIds);
        for (String peerId : precedingPeerIds) {
            setUpConnection(peerId);
        }

        System.out.println("Reached");

        // Listen for incoming connections
        listenForConnections();
    }

    /**
     * Establishes a connection with a specified peer.
     * Initializes a new Handler thread for the connection.
     *
     * @param peerId The ID of the peer to connect to.
     * @throws IOException If an I/O error occurs.
     */
    private static void setUpConnection(String peerId) throws IOException {
        int sPort = Integer.parseInt(peerInfoMap.get(peerId).peerPort);
        Socket requestSocket = new Socket(peerInfoMap.get(myPeerId).peerAddress, sPort);
        new Handler(requestSocket, myPeerId, peerId, bitfield, peerData).start();
    }

    /**
     * Initializes a server socket to listen for incoming connections.
     * Spawns a new Handler thread for each connection.
     *
     * @throws IOException If an I/O error occurs.
     */
    private static void listenForConnections() throws IOException {
        int sPort = Integer.parseInt(peerInfoMap.get(myPeerId).peerPort);
        try (ServerSocket listener = new ServerSocket(sPort)) {
            while (true) {
                Socket clientSocket = listener.accept();
                new Handler(clientSocket, myPeerId, "UNIDENTIFIED CLIENT", bitfield, peerData).start();
            }
        }
    }

    /**
     * Reads the bitfield information from PeerInfo.cfg and initializes the bitfield variable.
     * Also populates the precedingPeerIds list.
     *
     * @param common The Common configuration object.
     * @throws IOException If an I/O error occurs.
     */
    private static void readBitFieldFromPeerInfo(Common common) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"))) {
            String line;
            boolean isPeerFound = false;

            while ((line = in.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                String rowPeerId = tokens[0];

                if (rowPeerId.equals(myPeerId)) {
                    isPeerFound = true;

                    // Compute the bitfield size and initialize the bitfield
                    int bitfieldSize = Math.ceilDiv(common.fileSizeInBytes(), common.pieceSizeInBytes());
                    bitfield = "1".equals(tokens[3]) ? BitField.ones(bitfieldSize) : BitField.zeros(bitfieldSize);
                    break;
                }
                precedingPeerIds.add(rowPeerId);
            }

            if (!isPeerFound) {
                System.out.println("Error: Peer ID " + myPeerId + " not found!");
            }
        }
    }
}
