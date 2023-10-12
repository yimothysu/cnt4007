import java.util.ArrayList;

public class BitField {
    final ArrayList<Boolean> bits;

    public BitField(ArrayList<Boolean> bits) {
        this.bits = bits;
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

}
