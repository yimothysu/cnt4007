//Then the peer process starts and reads the file
//Common.cfg to set the corresponding
//variables. The peer process also reads the file PeerInfo.cfg . It will find that the [has file
//or not] fie ld is 1, which means it has the complete file, it sets all the bits of its bitfield to
//be 1. (On the other hand, if the [has file or not] field is 0, it sets all the bits of its bitfield
//to 0.) Here the bitfield is a data structure where your peer process manages the pieces.
//You have the freedom in how to implement it. This peer also finds out that it is the first
//peer; it will just listen on the port 6008 as specified in the file. Being the first peer, there
//are no other peer s to make connection s to.

public class peerProcess {
    public static void main(String[] args) {

    }
}
