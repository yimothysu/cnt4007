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
}