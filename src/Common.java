public record Common(
        int numberOfPreferredNeighbors,
        int unchokingIntervalInSeconds,
        int optimisticUnchokingIntervalInSeconds,
        String fileName,
        int fileSizeInBytes,
        int pieceSizeInBytes
        ) {
}
