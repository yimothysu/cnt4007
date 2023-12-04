import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A handler thread class.  Handlers are spawned from the listening
 * loop and are responsible for dealing with a single client's requests.
 */
class Handler extends Thread {
    private Socket connection;
    private ObjectInputStream in;    //stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
    private String myPeerId; // My ID
    private String peerId;  // The ID of the peer this Handler handles
    private BitField myBitField;

    private PeerData peerData;

    private Consumer<String> peerIdCallback = null;
    private Consumer<BroadcastCallbackArguments> broadcastCallback = null;

    public Handler(Socket connection, String myPeerId, String peerId, BitField myBitField, PeerData peerData) {
        this.connection = connection;
        this.peerId = peerId;
        this.myPeerId = myPeerId;
        this.myBitField = myBitField;
        this.peerData = peerData;
    }

    private PeerDatum getPeer(String peerId) {
        return peerData.peerDataByName.get(peerId);
    }

    public void setPeerIdCallback(Consumer<String> peerIdCallback) {
        this.peerIdCallback = peerIdCallback;
    }

    public void setBroadcastCallback(Consumer<BroadcastCallbackArguments> broadcastCallback) {
        this.broadcastCallback = broadcastCallback;
    }

    // The handshake header is 18-byte string
    //‘P2PFILESHARINGPROJ’, which is followed by 10-byte zero bits, which is followed by
    // 4-byte peer ID which is the integer representation of the peer ID.
    private void sendHandshake() throws IOException {
        out.write("P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8));
        out.write(new byte[10]);
        out.write(ByteBuffer.allocate(4).putInt(Integer.parseInt(myPeerId)).array());
        out.flush();
        System.out.println("Sent handshake!");
    }

    public void run() {
        try {
            // initialize Input and Output streams
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());
            sendHandshake();

            while (true) {
                byte[] message = new byte[32];
                in.readFully(message);
                System.out.println("Received message from Client " + peerId);
                if (isHandshake(message)) {
                    rcvHandshake(message);
                    break;
                }
            }

            while (true) {
                long startTime = System.currentTimeMillis();

                byte[] header = new byte[5];
                in.readFully(header);
                ParsedHeader parsedHeader = parseHeader(header);

                byte[] message = new byte[parsedHeader.length - 1];
                in.readFully(message);

                long endTime = System.currentTimeMillis();

                rcv(parsedHeader, message, endTime - startTime);
            }
        } catch (IOException ioException) {
            System.out.println("Disconnect with Client " + peerId);
            checkForTermination();
            ioException.printStackTrace();
        } finally {
            // Close connections
            try {
                //
                in.close();
                out.close();
                connection.close();
                checkForTermination();
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + peerId);
                checkForTermination();
            }
        }
    }

    private boolean isHandshake(byte[] msg) {
        return new String(msg, 0, 18).equals("P2PFILESHARINGPROJ");
    }

    // send a message with message type but no message content
    public void sendMessage(MsgType type) {
        sendMessage(type, new byte[0]);
        System.out.println("Sent message of type " + type + " to Client " + peerId);
    }

    public void sendMessage(MsgType type, byte[] msg) {
        try {
            // Convert the length to a 4-byte array
            int msgLength = msg.length + 1; // Adding 1 for the type byte

            // Send the message
            out.writeInt(msgLength);
            out.writeByte(type.getValue());
            out.write(msg);
            out.flush();

            System.out.println("Sent message of type " + type + " to Client " + peerId);
        } catch (IOException ioException) {
            // Handle the exception appropriately
            ioException.printStackTrace();
        }
    }


    record ParsedHeader(int length, int type) {}

    ParsedHeader parseHeader(byte[] msg) {
        // get an integer out of the first 4 bytes
        int length = ByteBuffer.wrap(msg, 0, 4).getInt();
        int type = ByteBuffer.wrap(msg, 4, 1).get();
        return new ParsedHeader(length, type);
    }

    private void rcv(ParsedHeader parsed, byte[] contents, long downloadTimeMs) {
//        message type value
//        choke 0
//        unchoke 1
//        interested 2
//        not interested 3
//        have 4
//        bitfield 5
//        request 6
//        piece 7
        // System.out.println("Parsed type is " + parsed.type);
        switch (parsed.type) {
            case 0:
                rcvChoke();
                break;
            case 1:
                rcvUnchoke();
                break;
            case 2:
                rcvInterested();
                break;
            case 3:
                rcvNotInterested();
                break;
            case 4:
                rcvHave(contents);
                break;
            case 5:
                rcvBitfield(contents);
                break;
            case 6:
                rcvRequest(contents);
                break;
            case 7:
                long downloadSizeB = parsed.length + 4;
                getPeer(peerId).addDownloadData(downloadSizeB, downloadTimeMs);
                rcvPiece(contents);
                break;
        }
    }

    // The handshake header is an 18-byte string
    // 'P2PFILESHARINGPROJ', followed by 10-byte zero bits, and then
    // a 4-byte peer ID representing the integer representation of the peer ID.
    private void rcvHandshake(byte[] msg) {
        // Check if the message length is not 32 bytes
        if (msg.length != 32) {
            System.out.println("Invalid message length!");
            return;
        }

        // Extract the header and peer ID from the message
        String header = new String(msg, 0, 18);
        int peerId = ByteBuffer.wrap(msg, 28, 4).getInt();
        this.peerId = Integer.toString(peerId);
        if (peerIdCallback != null) {
            peerIdCallback.accept(this.peerId);
        }

        // Check if the received header matches the expected header
        if ("P2PFILESHARINGPROJ".equals(header)) {
            System.out.println("Received valid handshake. Peer ID: " + peerId);
        } else {
            System.out.println("Received invalid handshake.");
        }

        // 'bitfield' messages are sent as the first message after handshaking when
        // a connection is established. 'bitfield' messages have a bitfield as their payload.
        // Each bit in the bitfield payload represents whether the peer has the corresponding piece or not.
        // The first byte of the bitfield corresponds to piece indices 0 – 7 from high bit to low bit,
        // respectively. The next one corresponds to piece indices 8 – 15, etc. Spare bits at the end
        // are set to zero. Peers that don’t have anything yet may skip a 'bitfield' message.
        if (!myBitField.isEmpty()) {
            sendMessage(MsgType.BITFIELD, myBitField.toByteArray());
            System.out.println("Sent bitfield: " + myBitField.bits.toString());
        }

        peerData.peerDataByName.put(this.peerId, new PeerDatum());
    }

    // Handle a received 'choke' message.
    // This method will be called when a 'choke' message is received from the peer.
    private void rcvChoke() {
        // Add your code here to handle the 'choke' message.
        getPeer(peerId).theyAreChokingUs();
        Logzzzzz.log("Peer " + myPeerId + " is choked by " + peerId + ".");
    }

    // Handle a received 'unchoke' message.
    // This method will be called when an 'unchoke' message is received from the peer.
    private void rcvUnchoke() {
        getPeer(peerId).theyAreNoLongerChokingUs();
        System.out.println("Received unchoke from peer " + peerId);
        Logzzzzz.log("Peer " + myPeerId + " is unchoked by " + peerId + ".");

        // If we are interested in the peer, send a 'request' message
        if (myBitField.interestedIn(getPeer(peerId).bitField)) {
            int pieceToRequest = myBitField.chooseRandomPieceToRequest(getPeer(peerId).bitField);
            sendMessage(MsgType.REQUEST, ByteBuffer.allocate(4).putInt(pieceToRequest).array());
            System.out.println("Sent request for piece " + pieceToRequest + " to peer " + peerId);
        }
    }

    // Handle a received 'interested' message.
    // This method will be called when an 'interested' message is received from the peer.
    private void rcvInterested() {
        getPeer(peerId).interested = true;
        Logzzzzz.log("Peer " + myPeerId + " received the 'interested' message from peer " + peerId + ".");
    }

    // Handle a received 'not interested' message.
    // This method will be called when a 'not interested' message is received from the peer.
    private void rcvNotInterested() {
        getPeer(peerId).interested = false;
        Logzzzzz.log("Peer " + myPeerId + " received the 'not interested' message from peer " + peerId + ".");
    }

    // Handle a received 'have' message.
    // This method will be called when a 'have' message is received from the peer.
    // ‘have’ messages have a payload that contains a 4-byte piece index field
    private void rcvHave(byte[] msg) {
        int pieceIndex = ByteConverter.fromByteArray(msg);
        Logzzzzz.log("Peer " + myPeerId + " received the 'have' message from peer " + peerId + " for piece " + pieceIndex + ".");

        boolean interestedBefore = myBitField.interestedIn(getPeer(peerId).bitField);
        getPeer(peerId).bitField.setBit(pieceIndex, true);
        boolean interestedAfter = myBitField.interestedIn(getPeer(peerId).bitField);

        if (!interestedBefore && interestedAfter) {
            sendMessage(MsgType.INTERESTED);
        }

        checkForTermination();
    }

    // Handle a received 'bitfield' message.
    // This method will be called when a 'bitfield' message is received from the peer.
    private void rcvBitfield(byte[] msg) {
        // Convert the received byte array to a BitField and store it
        BitField receivedBitfield = BitField.fromByteArray(msg);
        getPeer(peerId).bitField.setBitField(receivedBitfield.getBitfield());

        // If the peer is interested in the received bitfield, send an 'interested' message
        if (myBitField.interestedIn(receivedBitfield)) {
            sendMessage(MsgType.INTERESTED);
            System.out.println("Sent interested to peer " + peerId);
        }
    }

    // Handle a received 'request' message.
    // This method will be called when a 'request' message is received from the peer.
    // Break down the file into pieces and send the requested piece to the peer.
    private void rcvRequest(byte[] msg) {
        // Check if peer is choked; if so do nothing
        if (getPeer(peerId).isChoked()) {
            return;
        }

        int pieceIndex = ByteConverter.fromByteArray(Arrays.copyOfRange(msg, 0, 4));

        // Send piece to peer
        sendMessage(MsgType.PIECE, PieceManager.getPiece(pieceIndex));
    }

    // Handle a received 'piece' message.
    // This method will be called when a 'piece' message is received from the peer.
    //
    // 1. Update my own bitfield.
    // 2. Update interestedness in everybody else (only send not-interested messages).
    // 3. Send have messages to everyone else about this piece.
    // 4. Send another request
    // 5. Store the piece in a file
    private void rcvPiece(byte[] msg) {
        // First 4 bytes of msg are the piece index.
        // The rest of the bytes are the piece contents.
        int pieceIndex = ByteConverter.fromByteArray(Arrays.copyOfRange(msg, 0, 4));
        byte[] pieceContents = Arrays.copyOfRange(msg, 4, msg.length);

        // 1
        myBitField.setBit(pieceIndex, true);
        Logzzzzz.log("Peer " + myPeerId + " has downloaded the piece " + pieceIndex + " from " + peerId + ". Now the number of pieces it has is " + myBitField.getNumPieces() + ".");

        // 2 and 3
        ArrayList<String> peerIdsWeAreTellingAboutPiece = new ArrayList<>();
        ArrayList<String> peerIdsWeAreNoLongerInterestedIn = new ArrayList<>();
        for (Map.Entry<String, PeerDatum> entry : peerData.peerDataByName.entrySet()) {
            String key = entry.getKey();
            PeerDatum value = entry.getValue();

            myBitField.setBit(pieceIndex, false);
            boolean interestedBefore = myBitField.interestedIn(value.bitField);
            myBitField.setBit(pieceIndex, true);
            boolean interestedAfter = myBitField.interestedIn(value.bitField);


            peerIdsWeAreTellingAboutPiece.add(key); // we are telling everyone about our new piece
            if (interestedBefore && !interestedAfter) { // we are notifying only those we are newly-disinterested in
                // Tell peer "value" we are not interested
                peerIdsWeAreNoLongerInterestedIn.add(key);
            }
        }
        if (broadcastCallback == null) {
            System.out.println("You didn't set broadcastCallback, you doofus");
        }
        broadcastCallback.accept(new BroadcastCallbackArguments(peerIdsWeAreNoLongerInterestedIn, MsgType.NOT_INTERESTED, new byte[0]));
        broadcastCallback.accept(new BroadcastCallbackArguments(
                peerIdsWeAreTellingAboutPiece, MsgType.HAVE, ByteBuffer.allocate(4).putInt(pieceIndex).array()));

        // 4
        // If we are still interested in the peer, send another 'request' message
        if (myBitField.interestedIn(getPeer(peerId).bitField)) {
            int pieceToRequest = myBitField.chooseRandomPieceToRequest(getPeer(peerId).bitField);
            sendMessage(MsgType.REQUEST, ByteBuffer.allocate(4).putInt(pieceToRequest).array());
            System.out.println("Sent request for piece " + pieceToRequest + " to peer " + peerId);
        }

        // Create piece file
        PieceManager.storePiece(pieceIndex, pieceContents);

        // If we have all pieces, assemble into file
        if (myBitField.allOnes()) {
            onFileDownloadComplete();
        }
        checkForTermination();
    }

    private void onFileDownloadComplete() {
        Logzzzzz.log("I, peer " + myPeerId + ", have downloaded the complete file.");

        PieceManager.assembleFile();
    }

    private void checkForTermination() {
        // Check if all peers have all pieces
        if (!myBitField.allOnes()) {
            return;
        }

        if (peerData.peerDataByName.values().size() < 5) {
            for (String name : peerData.peerDataByName.keySet()) {
                System.out.println("There are only " + peerData.peerDataByName.values().size() + " peers. Not terminating.");
                if (!peerData.peerDataByName.get(name).bitField.allOnes()) {
                    System.out.println("Peer " + name + " does not have all pieces. Not terminating.");
                }
            }
            return;
        }

        for (PeerDatum peerDatum : peerData.peerDataByName.values()) {
            if (!peerDatum.bitField.allOnes()) {
                return;
            }
        }

        // Terminate
        System.out.println("All peers have all pieces. Terminating in 5 seconds.");

        // sleep 5 seconds
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            in.close();
            out.close();
            connection.close();
        } catch (IOException e) {}
        System.exit(0);
    }
}