package org.example.cpu;

import org.example.bus.DataBus;

public final class FlagsRegister extends Register {

    // Maschere bit-mask hardware stabili (Bit più significativi)
    public static final int MASK_Z = 0x80; // Bit 7: Zero
    public static final int MASK_N = 0x40; // Bit 6: Sottrazione
    public static final int MASK_H = 0x20; // Bit 5: Half-Carry
    public static final int MASK_C = 0x10; // Bit 4: Carry

    public FlagsRegister(DataBus soCData, DataBus aluBus1, DataBus aluBus2, DataBus internalData) {
        super(soCData, aluBus1, aluBus2, internalData);
    }

    /**
     * Override hardware-accurate:
     * Qualsiasi valore venga forzato nel registro F, i 4 bit bassi vengono azzerati (and 0xF0)
     */
    @Override
    public void set(int value) {
        super.set(value & 0xF0);
    }

    // --- METODI DI SUPPORTO PER LE ISTRUZIONI CONDIZIONALI (ALU / SALTI) ---

    public boolean isZeroSet() {
        return (get() & MASK_Z) != 0;
    }

    public boolean isSubtractSet() {
        return (get() & MASK_N) != 0;
    }

    public boolean isHalfCarrySet() {
        return (get() & MASK_H) != 0;
    }

    public boolean isCarrySet() {
        return (get() & MASK_C) != 0;
    }

    public void setZ(boolean b) {
        if (b) {
            set(get() | MASK_Z);
        } else {
            set(get() & ~MASK_Z);
        }
    }

    public void setN(boolean b) {
        if (b) {
            set(get() | MASK_N);
        } else {
            set(get() & ~MASK_N);
        }
    }

    public void setH(boolean halfCarry) {
        if (halfCarry) {
            set(get() | MASK_H);
        } else {
            set(get() & ~MASK_H);
        }
    }

    public void setC(boolean carry) {
        if (carry) {
            set(get() | MASK_C);
        } else {
            set(get() & ~MASK_C);
        }
    }
}