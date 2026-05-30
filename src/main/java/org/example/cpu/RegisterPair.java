package org.example.cpu;

public class RegisterPair {

    private final Register high;
    private final Register low;

    public RegisterPair(Register high, Register low) {
        this.high = high;
        this.low = low;
    }

    /**
     * Combina i due registri a 8-bit per restituire il valore a 16-bit.
     * Es: High = 0xC0, Low = 0x12 -> Risultato = 0xC012
     */
    public int get() {
        int hi = high.get() & 0xFF;
        int lo = low.get() & 0xFF;
        return (hi << 8) | lo;
    }

    /**
     * Prende un valore a 16-bit, lo spacca in due e aggiorna i registri interni.
     */
    public void set(int value) {
        int hi = (value >> 8) & 0xFF;
        int lo = value & 0xFF;

        high.setValue(hi);
        low.setValue(lo);
    }
}