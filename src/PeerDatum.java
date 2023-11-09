public class PeerDatum {
    public BitField bitfield;
    public boolean interested;
    private boolean optUnchoked;
    private boolean preferred;

    public PeerDatum(BitField bitfield) {
        this.bitfield = bitfield;
        this.interested = false;
        this.optUnchoked = false;
        this.preferred = false;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public boolean isChoked() {
        return !(preferred || optUnchoked);
    }

    public boolean isUnchoked() {
        return preferred || optUnchoked;
    }

    public boolean isOptUnchoked() {
        return optUnchoked;
    }

    public void choke() {
        this.preferred = false;
        this.optUnchoked = false;
    }

    public void prefer() {
        this.preferred = true;
    }

    public void admit() {
        this.optUnchoked = true;
    }
}
