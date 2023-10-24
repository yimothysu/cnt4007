import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CommonCfgReader {

    /**
     * Reads the Common.cfg file and returns a Common object containing the configuration.
     *
     * @return a Common object containing the configuration read from Common.cfg
     * @throws IOException if the file could not be read
     */
    public static Common read() throws IOException {
        // Declare variables to hold configuration parameters
        int numberOfPreferredNeighbors;
        int unchokingIntervalInSeconds;
        int optimisticUnchokingIntervalInSeconds;
        String fileName;
        int fileSizeInBytes;
        int pieceSizeInBytes;

        // Initialize the BufferedReader to read the Common.cfg file
        BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));

        // Read number of preferred neighbors from file
        String st = in.readLine();
        numberOfPreferredNeighbors = Integer.parseInt(st.split("\\s+")[1]);

        // Read unchoking interval in seconds from file
        st = in.readLine();
        unchokingIntervalInSeconds = Integer.parseInt(st.split("\\s+")[1]);

        // Read optimistic unchoking interval in seconds from file
        st = in.readLine();
        optimisticUnchokingIntervalInSeconds = Integer.parseInt(st.split("\\s+")[1]);

        // Read file name from file
        st = in.readLine();
        fileName = st.split("\\s+")[1];

        // Read file size in bytes from file
        st = in.readLine();
        fileSizeInBytes = Integer.parseInt(st.split("\\s+")[1]);

        // Read piece size in bytes from file
        st = in.readLine();
        pieceSizeInBytes = Integer.parseInt(st.split("\\s+")[1]);

        // Close the BufferedReader
        in.close();

        // Create and return a Common object containing the read configuration
        return new Common(
                numberOfPreferredNeighbors,
                unchokingIntervalInSeconds,
                optimisticUnchokingIntervalInSeconds,
                fileName,
                fileSizeInBytes,
                pieceSizeInBytes
        );
    }
}
