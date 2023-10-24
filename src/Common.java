/**
 * Immutable data structure representing common configuration settings
 * for a peer-to-peer network. The class encapsulates parameters like
 * number of preferred neighbors, intervals for various operations,
 * and file-related information.
 *
 * This class uses Java's record feature to automatically generate
 * accessors, hashCode, equals, and toString methods.
 */
public record Common(
        /**
         * Number of neighbors that a peer will send pieces to.
         */
        int numberOfPreferredNeighbors,

        /**
         * Time interval, in seconds, to reevaluate and possibly change
         * the preferred neighbors.
         */
        int unchokingIntervalInSeconds,

        /**
         * Time interval, in seconds, to change the optimistically
         * unchoked neighbor.
         */
        int optimisticUnchokingIntervalInSeconds,

        /**
         * Name of the file that is being shared across the network.
         */
        String fileName,

        /**
         * Size of the file, in bytes, that is being shared.
         */
        int fileSizeInBytes,

        /**
         * Size of each piece, in bytes, that the file is split into.
         */
        int pieceSizeInBytes
) {
        // The record class automatically takes care of constructors, getters,
        // equals, hashCode, and toString methods.
}
