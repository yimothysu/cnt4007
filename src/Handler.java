import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
    private String myPeerId; // Peer ID of sender
    private String peerId;        //The peer id of the client
    private BitField bitfield;

    private PeerData peerData;

    private Consumer<String> peerIdCallback = null;

    public Handler(Socket connection, String myPeerId, String peerId, BitField bitfield, PeerData peerData) {
        this.connection = connection;
        this.peerId = peerId;
        this.myPeerId = myPeerId;
        this.bitfield = bitfield;
        this.peerData = peerData;
    }

    public void setCallback(Consumer<String> peerIdCallback) {
        this.peerIdCallback = peerIdCallback;
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

            // byte[] message = new byte[N];
            // in.readFully(message);
            while (true) {
                byte[] message = new byte[32];
                in.readFully(message);
                System.out.println("Received message " + Arrays.toString(message));
                if (isHandshake(message)) {
                    rcvHandshake(message);
                    System.out.println("Received handshake");
                    break;
                }
            }
            while (true) {
                byte[] header = new byte[5];
                in.readFully(header);
                ParsedHeader parsedHeader = parseHeader(header);

                byte[] message = new byte[parsedHeader.length - 1];
                in.readFully(message);
                // String contents = new String(message, StandardCharsets.UTF_8);
                rcv(parsedHeader, message);
            }
        } catch (IOException ioException) {
            System.out.println("Disconnect with Client " + peerId);
        } finally {
            //Close connections
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + peerId);
            }
        }
    }

    private boolean isHandshake(byte[] msg) {
        return new String(msg, 0, 18).equals("P2PFILESHARINGPROJ");
    }

    //send a message to the output stream
    public void sendMessage(String msg) {
        try {
            out.writeObject(msg);
            out.flush();
            System.out.println("Send message: " + msg + " to Client " + peerId);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    //send a message to the output stream
    public void sendMessage(MsgType type, byte[] msg) {
        try {
            // Convert the length to a 4-byte array
            int msgLength = msg.length + 1; // Adding 1 for the type byte

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(msgLength);
            byte[] length = buffer.array();

            // Send the message
            out.write(length);
            ByteBuffer tp = ByteBuffer.allocate(1);
            tp.put(type.getValue());
            out.write(tp.array());
            out.write(msg); // Using write instead of writeObject for raw bytes
            out.flush();

            System.out.println("Send message: " + msg + " to Client " + peerId);
        } catch (IOException ioException) {
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

    private void rcv(ParsedHeader parsed, byte[] contents) {
//        message type value
//        choke 0
//        unchoke 1
//        interested 2
//        not interested 3
//        have 4
//        bitfield 5
//        request 6
//        piece 7
        System.out.println("Parsed type is " + parsed.type);
        switch (parsed.type) {
            case 0:
                rcvChoke(contents);
                break;
            case 1:
                rcvUnchoke(contents);
                break;
            case 2:
                rcvInterested(contents);
                break;
            case 3:
                rcvNotInterested(contents);
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
                rcvPiece(contents);
                break;
        }
    }

    // The handshake header is an 18-byte string
    // 'P2PFILESHARINGPROJ', followed by 10-byte zero bits, and then
    // a 4-byte peer ID representing the integer representation of the peer ID.
    private void rcvHandshake(byte[] msg) {
        System.out.println("Invoke rcvHandshake");

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
        if (!bitfield.isEmpty()) {
            sendMessage(MsgType.BITFIELD, bitfield.toByteArray());
            System.out.println("Sent bitfield: " + bitfield.bits.toString());
        }
    }

    // Handle a received 'choke' message.
    // This method will be called when a 'choke' message is received from the peer.
    private void rcvChoke(byte[] msg) {
        // Add your code here to handle the 'choke' message.
    }

    // Handle a received 'unchoke' message.
    // This method will be called when an 'unchoke' message is received from the peer.
    private void rcvUnchoke(byte[] msg) {
        // Add your code here to handle the 'unchoke' message.
    }

    // Handle a received 'interested' message.
    // This method will be called when an 'interested' message is received from the peer.
    private void rcvInterested(byte[] msg) {
        // Add your code here to handle the 'interested' message.
    }

    // Handle a received 'not interested' message.
    // This method will be called when a 'not interested' message is received from the peer.
    private void rcvNotInterested(byte[] msg) {
        // Add your code here to handle the 'not interested' message.
    }

    // Handle a received 'have' message.
    // This method will be called when a 'have' message is received from the peer.
    private void rcvHave(byte[] msg) {
        // Add your code here to handle the 'have' message.
    }

    // Handle a received 'bitfield' message.
    // This method will be called when a 'bitfield' message is received from the peer.
    private void rcvBitfield(byte[] msg) {
        System.out.println("Received bitfield from peer " + peerId);
        System.out.println(Arrays.toString(msg));

        // Store the received bitfield in peerData
        peerData.peerDataByName.put(peerId, new PeerDatum(bitfield));

        // Print peerData for debugging purposes
        System.out.println(peerData.peerDataByName);
        for (Map.Entry<String, PeerDatum> entry : peerData.peerDataByName.entrySet()) {
            String key = entry.getKey();
            PeerDatum value = entry.getValue();
        }
    }

    // Handle a received 'request' message.
    // This method will be called when a 'request' message is received from the peer.
    private void rcvRequest(byte[] msg) {
        // Add your code here to handle the 'request' message.
    }

    // Handle a received 'piece' message.
    // This method will be called when a 'piece' message is received from the peer.
    private void rcvPiece(byte[] msg) {
        // Add your code here to handle the 'piece' message.
    }

}