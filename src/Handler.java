import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    public Handler(Socket connection, String myPeerId, String peerId, BitField bitfield) {
        this.connection = connection;
        this.peerId = peerId;
        this.myPeerId = myPeerId;
        this.bitfield = bitfield;
    }

    // The handshake header is 18-byte string
    //‘P2PFILESHARINGPROJ’, which is followed by 10-byte zero bits, which is followed by
    // 4-byte peer ID which is the integer representation of the peer ID.
    private void sendHandshake() throws IOException {
        out.write("P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8));
        // String str = "Hello, World!";
        // byte[] byteArray = str.getBytes();
        out.write(new byte[10]);
        out.write(ByteBuffer.allocate(4).putInt(Integer.parseInt(myPeerId)).array());
        out.flush();
        System.out.println("Sent handshake!");
    }

    public void run() {
        try {

            //initialize Input and Output streams
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
                    break;
                }
            }
            while (true) {
                byte[] header = new byte[5];
                in.readAllBytes();
                ParsedHeader parsedHeader = parseHeader(header);

                byte[] message = new byte[parsedHeader.length];
                in.readAllBytes();
                String contents = new String(message, StandardCharsets.UTF_8);
                rcv(parsedHeader, contents);
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
    public void sendMessage(byte[] msg) {
        try {
            // Convert the length to a 4-byte array
            int msgLength = msg.length + 1; // Adding 1 for the type byte

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(msgLength);
            byte[] length = buffer.array();

            byte[] type = new byte[1]; // TODO

            // Send the message
            out.write(length);
            out.write(type);
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
        int type = ByteBuffer.wrap(msg, 4, 1).getInt();
        return new ParsedHeader(length, type);
    }

    private void rcv(ParsedHeader parsed, String contents) {
//        message type value
//        choke 0
//        unchoke 1
//        interested 2
//        not interested 3
//        have 4
//        bitfield 5
//        request 6
//        piece 7
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
// The handshake header is 18-byte string
//‘P2PFILESHARINGPROJ’, which is followed by 10-byte zero bits, which is followed by
// 4-byte peer ID which is the integer representation of the peer ID.
    private void rcvHandshake(byte[] msg) {
        System.out.println("Invoke rcvHandshake");
        if (msg.length != 32) {
            System.out.println("Invalid message length!");
            return;
        }

        String header = new String(msg, 0, 18);
        int peerId = ByteBuffer.wrap(msg, 28, 4).getInt();
        this.peerId = Integer.toString(peerId);

        if ("P2PFILESHARINGPROJ".equals(header)) {
            System.out.println("Received valid handshake. Peer ID: " + peerId);
        } else {
            System.out.println("Received invalid handshake.");
        }
        // ‘bitfield’ messages is only sent as the first message right after handshaking is done when
        // a connection is established. ‘bitfield’ messages have a bitfield as its payload. Each bit in
        //the bitfield payload represents whether the peer has the corresponding piece or not. The
        //first byte of the bitfield corresponds to piece indices 0 – 7 from high bit to low bit,
        //respectively. The next one corresponds to piece indices 8 – 15, etc. Spare bits at the end
        //are set to zero. Peers that don’t have anything yet may skip a ‘bitfield’ message.
        if (!bitfield.isEmpty()) {
            sendMessage(bitfield.toByteArray());
            System.out.println("Sent bitfield");
        }
    }

    private void rcvChoke(String msg) {

    }

    private void rcvUnchoke(String msg) {

    }

    private void rcvInterested(String msg) {

    }

    private void rcvNotInterested(String msg) {

    }

    private void rcvHave(String msg) {

    }

    private void rcvBitfield(String msg) {
        System.out.println("Received bitfield");
        System.out.println(msg);
    }

    private void rcvRequest(String msg) {

    }


    private void rcvPiece(String msg) {

    }
}