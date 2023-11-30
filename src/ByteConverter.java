/**
 * ByteConverter is a utility class that provides methods for converting between
 * different types and their byte array representation.
 */
public class ByteConverter {

    /**
     * Converts an integer to its byte array representation.
     *
     * @param toConvert The integer to convert.
     * @return A byte array representing the integer.
     */
    public static byte[] toByteArray(int toConvert) {
        // Create a byte array of size 4 (int is 4 bytes in Java)
        byte[] result = new byte[4];

        // Populate the byte array using bitwise operations
        result[0] = (byte) (toConvert >> 24); // Shift right by 24 bits and cast to byte
        result[1] = (byte) (toConvert >> 16); // Shift right by 16 bits and cast to byte
        result[2] = (byte) (toConvert >> 8);  // Shift right by 8 bits and cast to byte
        result[3] = (byte) toConvert;         // No shift needed, just cast to byte

        return result;
    }

    /**
     * Converts a String to its byte array representation.
     *
     * @param toConvert The String to convert.
     * @return A byte array representing the String.
     */
    public static byte[] toByteArray(String toConvert) {
        // Utilize Java's String getBytes method to convert String to byte array
        return toConvert.getBytes();
    }

    /**
     * Converts a byte array to its integer representation.
     * Assumes that the byte array has length of at least 4 bytes.
     *
     * @param byteArray The byte array to convert.
     * @return An integer representing the byte array.
     */
    public static int fromByteArray(byte[] byteArray) {
        // Initialize the result variable
        int result = 0;

        // Use bitwise operations to convert byte array to integer
        result |= (byteArray[0] & 0xFF) << 24; // Shift left by 24 bits and OR with result
        result |= (byteArray[1] & 0xFF) << 16; // Shift left by 16 bits and OR with result
        result |= (byteArray[2] & 0xFF) << 8;  // Shift left by 8 bits and OR with result
        result |= (byteArray[3] & 0xFF);       // No shift needed, just OR with result

        return result;
    }
}
