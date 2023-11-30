import java.util.ArrayList;

public class PeerDatum {
    public BitField bitField;
    public boolean interested;
    private boolean optUnchoked;
    private boolean preferred;

    private long totalDownloadSizeB;
    private long totalDownloadTimeMs;

    private boolean theyBeChokingUs;

    public PeerDatum() {
        this.bitField = new BitField(new ArrayList<>());
        this.interested = false;
        this.optUnchoked = false;
        this.preferred = false;

        this.totalDownloadSizeB = 0;
        this.totalDownloadTimeMs = 0;

        this.theyBeChokingUs = true;
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

    public void resetOptimisticallyUnchoked() {
        this.optUnchoked = false;
    }

    public void prefer() {
        this.preferred = true;
    }

    public void admit() {
        this.optUnchoked = true;
    }

    public void theyAreChokingUs() { this.theyBeChokingUs = true; }

    public void theyAreNoLongerChokingUs() { this.theyBeChokingUs = false; }

    public void addDownloadData(long downloadSizeB, long downloadTimeMs) {
        this.totalDownloadSizeB += downloadSizeB;
        this.totalDownloadTimeMs += downloadTimeMs;
    }

    public void resetDownloadData() {
        this.totalDownloadSizeB = 0;
        this.totalDownloadTimeMs = 0;
    }

    public float getDownloadSpeed() {
        return (float) totalDownloadSizeB / totalDownloadTimeMs;
    }
}
