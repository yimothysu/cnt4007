import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PeerData {
    public HashMap<String, PeerDatum> peerDataByName = new HashMap<>();
    private Random rand = new Random("haha".hashCode());

    public record NeighborSelectionData(List<String> toChoke, List<String> toUnchoke) {
    }


    public NeighborSelectionData selectPreferredNeighbors(int preferredNeighborCount) {
        // Filter and collect interested peers
        List<PeerDatum> interestedPeers = peerDataByName.values().stream()
                .filter(peer -> peer.interested)
                .collect(Collectors.toList());

        // Shuffle the list to randomize the order
        Collections.shuffle(interestedPeers);

        // Sort interested peers by download speed in nonincreasing order
        interestedPeers.sort((a, b) -> Double.compare(b.getDownloadSpeed(), a.getDownloadSpeed()));

        // If there are fewer or equal interested peers than the preferred count, select them all
        if (interestedPeers.size() <= preferredNeighborCount) {
            return handlePreferredNeighborSelection(new HashSet<>(interestedPeers));
        }

        // Otherwise, select the top preferredNeighborCount peers
        List<PeerDatum> selectedPeers = interestedPeers.subList(0, preferredNeighborCount);
        return handlePreferredNeighborSelection(new HashSet<>(selectedPeers));
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
                }
            }
            else {
                if (peer.isPreferred()) {
                    toChoke.add(peerId);
                    peer.choke();
                }
            }
        }

        return new NeighborSelectionData(toChoke, toUnchoke);
    }
}
