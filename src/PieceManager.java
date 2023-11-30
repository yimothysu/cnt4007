import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PieceManager {
    private final static String PEER_DIRECTORY_PREFIX = "peer_";
    private final static String PIECES_DIRECTORY_NAME = "pieces";
    private final static String PIECE_FILE_PREFIX = "piece_";

    private static String rootPath;
    private static String piecesDirPath;
    public static int bitfieldSize = -1;
    public static String initialFileName = "";

    public static void init(String peerId) {
        PieceManager.rootPath = System.getProperty("user.dir") + File.separator + ".." + File.separator + PEER_DIRECTORY_PREFIX + peerId;
        PieceManager.piecesDirPath = rootPath + File.separator + PIECES_DIRECTORY_NAME;
        createDirectories();
    }

    private static void createDirectories() {
        // peer_1001 and peer_1001/pieces
        File dir2 = new File(piecesDirPath);
        if (!dir2.exists()) {
            boolean result = dir2.mkdirs();
            if (!result) {
                System.out.println("Failed to create directories");
            }
        }
    }

    public static void breakIntoPieces() {
        // For clients with the entire file on startup, break into pieces
        int numberOfPieces = bitfieldSize;

        File inputFile = new File(rootPath + File.separator + initialFileName);
        long fileSize = inputFile.length();
        long pieceSize = fileSize / numberOfPieces;
        long remainingBytes = fileSize % pieceSize;

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            for (int piece = 0; piece < numberOfPieces; piece++) {
                String pieceFileName = getPieceFilePath(piece);
                try (FileOutputStream fos = new FileOutputStream(pieceFileName)) {
                    long writtenBytes = 0;
                    while (writtenBytes < pieceSize && (bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        writtenBytes += bytesRead;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Handle any remaining bytes for the last piece
            if (remainingBytes > 0) {
                String pieceFileName = getPieceFilePath(numberOfPieces);
                try (FileOutputStream fos = new FileOutputStream(pieceFileName)) {
                    long writtenBytes = 0;
                    while (writtenBytes < remainingBytes && (bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        writtenBytes += bytesRead;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void storePiece(int pieceIndex, byte[] pieceContents) {
        // Store in peer_[peer_id]/pieces

        String fileName = PIECE_FILE_PREFIX + pieceIndex;
        try (FileOutputStream fileOutputStream = new FileOutputStream(rootPath + File.separator + PIECES_DIRECTORY_NAME + File.separator + fileName)) {
            fileOutputStream.write(pieceContents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] getPiece(int pieceIndex) {
        byte[] pieceContent = getPieceContent(pieceIndex);

        // Allocate buffer for 4 bytes of the index and the length of the piece content
        ByteBuffer buffer = ByteBuffer.allocate(4 + pieceContent.length);

        buffer.putInt(pieceIndex);
        buffer.put(pieceContent);

        return buffer.array();
    }


    public static byte[] getPieceContent(int pieceIndex) {
        // Read from peer_[peer_id]/pieces/piece_[piece_Index]

        String filePath = getPieceFilePath(pieceIndex);
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            return fileInputStream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getPieceFileName(int pieceIndex) {
        return PIECE_FILE_PREFIX + pieceIndex;
    }

    private static String getPieceFilePath(int pieceIndex) {
        return rootPath + File.separator + PIECES_DIRECTORY_NAME + File.separator + getPieceFileName(pieceIndex);
    }

    public static void assembleFile() {

        // Concatenate all of the pieces into the output file
        // Only call this function once all the pieces are downloaded
        File piecesDirectory = new File(rootPath + File.separator + PIECES_DIRECTORY_NAME);
        File[] pieceFiles = piecesDirectory.listFiles((dir, name) -> name.matches(PIECE_FILE_PREFIX + "\\d+"));

        if (pieceFiles != null && pieceFiles.length > 0) {
            Arrays.sort(pieceFiles, Comparator.comparingInt(PieceManager::extractPieceNumber));

            try (FileOutputStream fos = new FileOutputStream(rootPath + File.separator + initialFileName)) {
                for (File file : pieceFiles) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static int extractPieceNumber(File file) {
        Matcher matcher = Pattern.compile("\\d+").matcher(file.getName());
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

}
