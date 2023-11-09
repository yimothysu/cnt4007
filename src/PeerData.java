import java.util.*;

public class PeerData {
    public HashMap<String, PeerDatum> peerDataByName = new HashMap<>();

    private Random rand = new Random();


    public record NeighborSelectionData(List<String> toChoke, List<String> toUnchoke) {
    }

    /**
     * 1. Filter peerDataByName -> interested neighbors
     * 2. Randomly select preferred neighbors among interested neighbors
     */
    public NeighborSelectionData randomlySelectPreferredNeighbors(int preferredNeighborCount) {
        // 1
        ArrayList<PeerDatum> interestedPeers = new ArrayList<>();
        for (PeerDatum peer : peerDataByName.values()) {
            if (peer.interested) {
                interestedPeers.add(peer);
            }
        }

        // 2
        int removeCount = interestedPeers.size() - preferredNeighborCount;
        for (int i = 0; i < removeCount; i++) {
            interestedPeers.remove(rand.nextInt(interestedPeers.size()));
        }

        return selectPreferredNeighbors(new HashSet<>(interestedPeers));
    }

    public String selectOptimisticallyUnchokedNeighbor() {
        // Reset all peers that were optimistically unchoked
        peerDataByName.forEach((key, peer) -> {
            if (peer.isOptUnchoked()) {
            }
        });

        // Build a list of interested and choked peers
        List<String> interestedAndChokedPeers = peerDataByName.entrySet().stream()
                .filter(entry -> entry.getValue().interested && entry.getValue().isChoked())
                .map(Map.Entry::getKey)
                .toList();

        // If there are no interested and choked peers, return null
        if (interestedAndChokedPeers.isEmpty()) {
            return null;
        }

        // Randomly select one peer from the list
        return interestedAndChokedPeers.get(rand.nextInt(interestedAndChokedPeers.size()));
    }

    private NeighborSelectionData selectPreferredNeighbors(Set<PeerDatum> preferredNeighbors) {
        List<String> toChoke = new ArrayList<>();
        List<String> toUnchoke = new ArrayList<>();
        System.out.println( peerDataByName.entrySet());
        for (Map.Entry<String, PeerDatum> entry : peerDataByName.entrySet()) {
            String peerId = entry.getKey();
            PeerDatum peer = entry.getValue();
            if (preferredNeighbors.contains(peer)) {
                if (peer.isChoked()) {
                    toUnchoke.add(peerId);
                    peer.prefer();
                    System.out.println("TEMP LOG: PREFERRING PEER " + peerId);
                }
            }
            else {
                if (peer.isUnchoked()) {
                    toChoke.add(peerId);
                    peer.choke();
                    System.out.println("TEMP LOG: CHOKING PEER " + peerId);
                }
            }
        }
        return new NeighborSelectionData(toChoke, toUnchoke);
    }
}
