import java.util.ArrayList;
import java.util.Random;

public class BitField {
    private static final Random rand = new Random();
    ArrayList<Boolean> bits;

    public BitField(ArrayList<Boolean> bits) {
        this.bits = bits;
    }

    public void setBitField(ArrayList<Boolean> bits) {
        this.bits = bits;
    }

    public ArrayList<Boolean> getBitfield() {
        return this.bits;
    }

    public static BitField zeros(int size) {
        ArrayList<Boolean> zeros = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            zeros.add(false);
        }
        return new BitField(zeros);
    }

    public static BitField ones(int size) {
        ArrayList<Boolean> ones = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ones.add(true);
        }
        return new BitField(ones);
    }

    public boolean isEmpty() {
        for (Boolean bit : bits ) {
            if (bit) {
                return false;
            }
        }
        return true;
    }

    public boolean getBit(int index) {
        return bits.get(index);
    }

    public void setBit(int index, boolean value) {
        bits.set(index, value);
    }

    public byte[] toByteArray() {
        int size = bits.size();
        int byteLength = (size + 7) / 8; // Round up to nearest byte
        byte[] byteArray = new byte[byteLength];

        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                byteArray[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }

        return byteArray;
    }

    public static BitField fromByteArray(byte[] byteArray) {
        ArrayList<Boolean> bits = new ArrayList<>();
        for (int i = 0; i < PieceManager.bitfieldSize; i++) {
            int byteIndex = i / 8;
            int bitIndex = 7 - (i % 8);
            boolean bit = (byteArray[byteIndex] & (1 << bitIndex)) != 0;
            bits.add(bit);
        }
        return new BitField(bits);
    }
    
    // Returns true if the other bitfield contains a bit that this bitfield does not.
    // Helper method to get indices where this bitfield is false and other is true
    private ArrayList<Integer> getInterestedIndices(BitField other) {
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < bits.size(); i++) {
            if (!bits.get(i) && other.bits.get(i)) {
                indices.add(i);
            }
        }
        return indices;
    }

    public boolean interestedIn(BitField other) {
        return !getInterestedIndices(other).isEmpty();
    }

    public int chooseRandomPieceToRequest(BitField other) {
        ArrayList<Integer> indices = getInterestedIndices(other);
        if (!indices.isEmpty()) {
            return indices.get(rand.nextInt(indices.size()));
        }
        throw new IllegalStateException("No pieces to request.");
    }

    public int getNumPieces() {
        int numPieces = 0;
        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i)) {
                numPieces++;
            }
        }
        return numPieces;
    }

    public boolean allOnes() {
        return getNumPieces() == bits.size();
    }
}
