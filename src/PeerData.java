import java.util.*;

public class PeerData {
    public HashMap<String, PeerDatum> peerDataByName = new HashMap<>();
    private Random rand = new Random("haha".hashCode());

    public record NeighborSelectionData(List<String> toChoke, List<String> toUnchoke) {
    }


    public NeighborSelectionData selectPreferredNeighbors(int preferrredNeighborCount) {
        ArrayList<PeerDatum> interestedPeers = new ArrayList<>();
        for (PeerDatum peer : peerDataByName.values()) {
            if (peer.interested) {
                interestedPeers.add(peer);
            }
        }

        // Sort interested peers by download speed in nonincreasing order
        interestedPeers.sort((a, b) -> Double.compare(b.getDownloadSpeed(), a.getDownloadSpeed()));

        /*
            We handle ties by randomly selecting peers with the same download speeds.

            We only have to handle ties when there exists an interval [alpha, beta] of
            peers in the interested peers list such that each peer in the interval has
            the same download speed and such that preferredNeighborCount - 1 lies
            in [alpha, beta - 1].
         */
        if (interestedPeers.size() <= preferrredNeighborCount) {
            return handlePreferredNeighborSelection(new HashSet<>(interestedPeers));
        }

        // Compute beta
        int beta = interestedPeers.size();
        float gamma = interestedPeers.get(beta - 1).getDownloadSpeed();
        while (interestedPeers.get(beta).getDownloadSpeed() < gamma) {
            --beta;
        }

        /*
            In this case we do not have to handle tiebreaker since the interval [alpha, beta]
            lies entirely inside [0, preferredNeighborCount - 1].
         */
        if (beta == preferrredNeighborCount - 1) {
            return handlePreferredNeighborSelection(new HashSet<>(interestedPeers));
        }

        // Compute alpha
        int alpha = beta;
        while (alpha > 0 && interestedPeers.get(alpha - 1).getDownloadSpeed() == gamma) {
            --alpha;
        }

        // Construct interestedPeers and copy over peers [0, alpha) from interested peers list
        ArrayList<PeerDatum> peersToSelect = new ArrayList<>();
        for (int i = 0; i < alpha; ++i) {
            peersToSelect.add(interestedPeers.get(i));
        }

        // Number of peers to randomly select from interval [alpha, beta]
        int M = preferrredNeighborCount - alpha;

        // Peers in interval [alpha, beta] in interested peers list
        ArrayList<PeerDatum> borderPeers = new ArrayList<>();
        for (int i = alpha; i <= beta; ++i) {
            borderPeers.add(interestedPeers.get(i));
        }

        // Randomly select M peers from borderPeers list by removal
        int removeCount = borderPeers.size() - M;
        for (int i = 0; i < removeCount; i++) {
            borderPeers.remove(rand.nextInt(borderPeers.size()));
        }

        // Merge borderPeers into peersToSelect list
        peersToSelect.addAll(borderPeers);

        return handlePreferredNeighborSelection(new HashSet<>(peersToSelect));
    }

    /**
     * 1. Filter peerDataByName -> interested neighbors
     * 2. Randomly select preferred neighbors among interested neighbors
     */
    /*
    public NeighborSelectionData randomlySelectPreferredNeighbors(int preferredNeighborCount) {
        // 1
        ArrayList<PeerDatum> interestedPeers = new ArrayList<>();
        for (PeerDatum peer : peerDataByName.values()) {
            if (peer.interested) {
                interestedPeers.add(peer);
            }
        }

        // 2
        int removeCount = Math.max(0, interestedPeers.size() - preferredNeighborCount);
        for (int i = 0; i < removeCount; i++) {
            interestedPeers.remove(rand.nextInt(interestedPeers.size()));
        }

        return selectPreferredNeighbors(new HashSet<>(interestedPeers));
    }
     */

    public String selectOptimisticallyUnchokedNeighbor() {
        // Reset all peers that were optimistically unchoked
        peerDataByName.forEach((key, peer) -> {
            if (peer.isOptUnchoked()) {
                peer.resetOptimisticallyUnchoked();
            }
        });

        // Build a list of interested and choked peers
        List<String> interestedAndChokedPeers = peerDataByName.entrySet().stream()
                .filter(entry -> entry.getValue().interested && entry.getValue().isChoked())
                .map(Map.Entry::getKey)
                .toList();
        // print every peer and whether they are interested or not and choked or not
        // Print status of each peer
        peerDataByName.forEach((key, peer) -> {
            String interestStatus = peer.interested ? "Interested" : "Not Interested";
            String chokeStatus = peer.isChoked() ? "Choked" : "Unchoked";
            System.out.println("Peer " + key + ": " + interestStatus + ", " + chokeStatus);
        });

        // If there are no interested and choked peers, return null
        if (interestedAndChokedPeers.isEmpty()) {
            return null;
        }

        // Randomly select one peer from the list
        String peerToOptUnchoke =
                interestedAndChokedPeers.get(rand.nextInt(interestedAndChokedPeers.size()));
        peerDataByName.get(peerToOptUnchoke).admit();
        return peerToOptUnchoke;
    }

    private NeighborSelectionData handlePreferredNeighborSelection(Set<PeerDatum> neighborToMakePreferred) {
        List<String> toChoke = new ArrayList<>();
        List<String> toUnchoke = new ArrayList<>();

        for (Map.Entry<String, PeerDatum> entry : peerDataByName.entrySet()) {
            String peerId = entry.getKey();
            PeerDatum peer = entry.getValue();
            if (neighborToMakePreferred.contains(peer)) {
                if (peer.isChoked()) {
                    toUnchoke.add(peerId);
                    peer.prefer();
                    System.out.println("TEMP LOG: PREFERRING PEER " + peerId);

                }
            }
            else {
                if (peer.isPreferred()) {
                    toChoke.add(peerId);
                    peer.choke();
                    System.out.println("TEMP LOG: CHOKING PEER " + peerId);
                }
            }
        }

        return new NeighborSelectionData(toChoke, toUnchoke);
    }
}
