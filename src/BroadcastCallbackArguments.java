import java.util.ArrayList;
import java.util.List;

public class BroadcastCallbackArguments {
    public List<String> peerIds;
    public MsgType msgType;
    public byte[] payload;

    public BroadcastCallbackArguments(List<String> peerIds, MsgType msgType, byte[] payload) {
        this.peerIds = peerIds;
        this.msgType = msgType;
        this.payload = payload;
    }

}
