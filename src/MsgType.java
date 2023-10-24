/**
 * Enumeration to represent the different types of messages that can be sent or received
 * in a peer-to-peer network.
 *
 * Each message type is associated with an integer value, represented as a byte.
 */
public enum MsgType {

    // Message types with their corresponding integer values
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOT_INTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7);

    // The integer value of the message type, stored as a byte
    private final byte value;

    /**
     * Private constructor for initializing the enum instances.
     *
     * @param value The integer value corresponding to the message type.
     */
    MsgType(int value) {
        this.value = (byte) value;
    }

    /**
     * Gets the integer value of the message type.
     *
     * @return The integer value as a byte.
     */
    public byte getValue() {
        return value;
    }

    /**
     * Returns the message type that corresponds to a given integer value.
     *
     * @param value The integer value as a byte.
     * @return The corresponding message type.
     */
    public static MsgType fromInt(byte value) {
        // Use the byte value as an index to get the corresponding enum constant
        return new MsgType[]{CHOKE, UNCHOKE, INTERESTED, NOT_INTERESTED, HAVE, BITFIELD, REQUEST, PIECE}[value];
    }
}
