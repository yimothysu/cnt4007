import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CommonCfgReader {
    public static Common read() throws IOException {
        int numberOfPreferredNeighbors;
        int unchokingIntervalInSeconds;
        int optimisticUnchokingIntervalInSeconds;
        String fileName;
        int fileSizeInBytes;
        int pieceSizeInBytes;

        BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));

        String st;
        st = in.readLine();
        numberOfPreferredNeighbors = Integer.parseInt(st.split("\\s+")[1]);

        st = in.readLine();
        unchokingIntervalInSeconds = Integer.parseInt(st.split("\\s+")[1]);

        st = in.readLine();
        optimisticUnchokingIntervalInSeconds = Integer.parseInt(st.split("\\s+")[1]);

        st = in.readLine();
        fileName = st.split("\\s+")[1];

        st = in.readLine();
        fileSizeInBytes = Integer.parseInt(st.split("\\s+")[1]);

        st = in.readLine();
        pieceSizeInBytes = Integer.parseInt(st.split("\\s+")[1]);

        in.close();

        return new Common(numberOfPreferredNeighbors,
                unchokingIntervalInSeconds,
                optimisticUnchokingIntervalInSeconds,
                fileName,
                fileSizeInBytes,
                pieceSizeInBytes);
    }
}
