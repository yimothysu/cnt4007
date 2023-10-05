import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * A handler thread class.  Handlers are spawned from the listening
 * loop and are responsible for dealing with a single client's requests.
 */
class Handler extends Thread {
    private Socket connection;
    private ObjectInputStream in;    //stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
    private String peerId;        //The peer id of the client

    public Handler(Socket connection, String peerId) {
        this.connection = connection;
        this.peerId = peerId;
    }

    private void sendHandshake() {
        sendMessage("P2PFILESHARINGPROJ asdf");
    }

    public void run() {
        try {

            //initialize Input and Output streams
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());
            sendHandshake();
            while (true) {
                byte[] message = in.readAllBytes();
                if (message.length == 0) {
                    continue;
                }
                if (isHandshake(message)) {
                    String contents = new String(message, 18, message.length - 18);
                    rcvHandshake(contents);
                    break;
                }
            }
            while (true) {
                byte[] message = in.readAllBytes();
                rcv(message);
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

    record ParsedMsg(int length, int type, String contents) {}

    ParsedMsg parseMsg(byte[] msg) {
        // get an integer out of the first 4 bytes
        int length = ByteBuffer.wrap(msg, 0, 4).getInt();
        int type = ByteBuffer.wrap(msg, 4, 1).getInt();
        String contents = new String(msg, 5, length - 1);
        return new ParsedMsg(length, type, contents);
    }

    private void rcv(byte[] msg) {
//        message type value
//        choke 0
//        unchoke 1
//        interested 2
//        not interested 3
//        have 4
//        bitfield 5
//        request 6
//        piece 7
        ParsedMsg parsed = parseMsg(msg);
        switch (parsed.type) {
            case 0:
                rcvChoke(parsed.contents);
                break;
            case 1:
                rcvUnchoke(parsed.contents);
                break;
            case 2:
                rcvInterested(parsed.contents);
                break;
            case 3:
                rcvNotInterested(parsed.contents);
                break;
            case 4:
                rcvHave(parsed.contents);
                break;
            case 5:
                rcvBitfield(parsed.contents);
                break;
            case 6:
                rcvRequest(parsed.contents);
                break;
            case 7:
                rcvPiece(parsed.contents);
                break;
        }
    }

    private void rcvHandshake(String msg) {
        System.out.println("HANDSKAE");
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

    }

    private void rcvRequest(String msg) {

    }


    private void rcvPiece(String msg) {

    }
}