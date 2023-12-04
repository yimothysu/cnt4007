import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The peerProcess class is the entry point for each peer in the network.
 * It is responsible for establishing connections and handling data transfers.
 */
public class peerProcess {
    static String myPeerId;
    static BitField myBitField;
    static HashMap<String, RemotePeerInfo> peerInfoMap;
    static PeerData peerData;
    static Common common;

    // An array list to store the IDs of peers that precede this peer
    static ArrayList<String> precedingPeerIds = new ArrayList<>();

    static HashMap<String, Handler> peerHandlers;

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
        // create the directories ...
        PieceManager.init(myPeerId);

        Logzzzzz.initWithPeerId(myPeerId);
        common = CommonCfgReader.read();
        peerInfoMap = PeerInfoCfgReader.read();
        readBitFieldFromPeerInfo(common);

        peerData = new PeerData();
        peerHandlers = new HashMap<>();

        // Establish connections with peers that precede this one in the network
        for (String peerId : precedingPeerIds) {
            setUpConnection(peerId);
        }

        // Select preferred neighbors every K seconds
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable selectPreferredTask = () -> {
            PeerData.NeighborSelectionData neighborSelectionData
                    = peerData.selectPreferredNeighbors(common.numberOfPreferredNeighbors());

            if (neighborSelectionData.toUnchoke().size() + neighborSelectionData.toChoke().size() > 0) {
                StringBuilder serializedNeighborsListForLogging = new StringBuilder();
                String delimiter = "";
                for (String str : neighborSelectionData.toChoke()) {
                    serializedNeighborsListForLogging.append(delimiter).append(str);
                    delimiter = ", ";
                }
                for (String str : neighborSelectionData.toUnchoke()) {
                    serializedNeighborsListForLogging.append(delimiter).append(str);
                    delimiter = ", ";
                }
                Logzzzzz.log("Peer " + myPeerId + " has the preferred neighbors " + serializedNeighborsListForLogging);
            }

            for (String peerId : neighborSelectionData.toChoke()) {
                getPeerHandler(peerId).sendMessage(MsgType.CHOKE);
            }
            for (String peerId : neighborSelectionData.toUnchoke()) {
                getPeerHandler(peerId).sendMessage(MsgType.UNCHOKE);
            }

            for (PeerDatum peerDatum : peerData.peerDataByName.values()) {
                peerDatum.resetDownloadData();
            }
        };
        // Schedules the task, following the initial delay of 2 seconds (arbitrarily chosen).
        executor.scheduleAtFixedRate(selectPreferredTask, 2, common.unchokingIntervalInSeconds(), TimeUnit.SECONDS);


        // Select optimistically unchoked neighbor every U seconds
        Runnable selectOptUnchoked = () -> {
            String peerOptUnchoked = peerData.selectOptimisticallyUnchokedNeighbor();

            if (peerOptUnchoked != null) {
                getPeerHandler(peerOptUnchoked).sendMessage(MsgType.UNCHOKE);
                Logzzzzz.log("Peer " + myPeerId + " has the optimistically unchoked neighbor " + peerOptUnchoked + ".");
            }
        };
        // Schedules the task, following the initial delay of 2 seconds.
        executor.scheduleAtFixedRate(selectOptUnchoked, 2, common.optimisticUnchokingIntervalInSeconds(), TimeUnit.SECONDS);

        // Listen for incoming connections
        listenForConnections();
    }

    public static Handler getPeerHandler(String peer) {
        return peerHandlers.get(peer);
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
        Socket requestSocket = new Socket(peerInfoMap.get(peerId).peerAddress, sPort);
        Handler handler = new Handler(requestSocket, myPeerId, peerId, myBitField, peerData);
        handler.setBroadcastCallback((BroadcastCallbackArguments arguments) -> {
            for (String peerIdToSendTo : arguments.peerIds) {
                getPeerHandler(peerIdToSendTo).sendMessage(arguments.msgType, arguments.payload);
            }
        });
        handler.start();
        peerHandlers.put(peerId, handler);
        Logzzzzz.log("Peer " + myPeerId + " makes a connection to Peer " + peerId + ".");
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
                Handler handler = new Handler(clientSocket, myPeerId, "UNIDENTIFIED CLIENT", myBitField, peerData);
                handler.setPeerIdCallback((String peerId) -> {
                    peerHandlers.put(peerId, handler);
                    Logzzzzz.log("Peer " + myPeerId + " makes a connection to Peer " + peerId + ".");
                });
                handler.setBroadcastCallback((BroadcastCallbackArguments arguments) -> {
                    for (String peerId : arguments.peerIds) {
                        getPeerHandler(peerId).sendMessage(arguments.msgType, arguments.payload);
                    }
                });
                handler.start();

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
        PieceManager.initialFileName = common.fileName();

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
                    PieceManager.bitfieldSize = bitfieldSize;
                    myBitField = "1".equals(tokens[3]) ? BitField.ones(bitfieldSize) : BitField.zeros(bitfieldSize);
                    if (myBitField.allOnes()) {
                        PieceManager.breakIntoPieces();
                    }
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
