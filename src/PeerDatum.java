public class PeerDatum {
    public BitField bitfield;
    public boolean choked;
    public boolean interested;

    public PeerDatum(BitField bitfield) {
        this.bitfield = bitfield;
        this.choked = true;
        this.interested = false;
    }

}
