package org.example.cpu;

import org.example.Main;
import org.example.bus.BusWriter;
import org.example.bus.DataBus;
import org.example.bus.data.ByteData;

public class Alu implements BusWriter {

    private final DataBus soCData;
    private final DataBus regToAluBus1;
    private final DataBus regToAluBus2;

    // Maschere hardware per i flag nel registro F (SM83)
    private static final int FLAG_Z = 0x80;
    private static final int FLAG_N = 0x40;
    private static final int FLAG_H = 0x20;
    private static final int FLAG_C = 0x10;

    public Alu(DataBus soCData, DataBus regToAluBus1, DataBus regToAluBus2) {
        this.soCData = soCData;
        this.regToAluBus1 = regToAluBus1;
        this.regToAluBus2 = regToAluBus2;
    }

    // ==========================================
    //          OPERAZIONI ARITMETICHE
    // ==========================================

    /**
     * ADD A, s -> Somma op1 e op2. Spedisce su SoCData.
     */
    public int add() {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int op2 = regToAluBus2.sampleByte() & 0xFF;
        int result = (op1 + op2) & 0xFF;

        int newFlags = 0;
        if (result == 0) newFlags |= FLAG_Z;
        if (((op1 & 0x0F) + (op2 & 0x0F)) > 0x0F) newFlags |= FLAG_H;
        if ((op1 + op2) > 0xFF) newFlags |= FLAG_C;

        if(Main.LOG_ALU_ENABLED)
            System.out.printf("[ALU ADD] 0x%02X + 0x%02X = 0x%02X | Flags Generati: %s (Raw: 0x%02X)\n",
                    op1, op2, result, getAluFlagsString(newFlags), newFlags);

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * ADC A, s -> Somma op1, op2 e il Carry in ingresso. Spedisce su SoCData.
     */
    public int adc(boolean carryIn) {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int op2 = regToAluBus2.sampleByte() & 0xFF;
        int cy = carryIn ? 1 : 0;
        int result = (op1 + op2 + cy) & 0xFF;

        int newFlags = 0;
        if (result == 0) newFlags |= FLAG_Z;
        if (((op1 & 0x0F) + (op2 & 0x0F) + cy) > 0x0F) newFlags |= FLAG_H;
        if ((op1 + op2 + cy) > 0xFF) newFlags |= FLAG_C;

        if(Main.LOG_ALU_ENABLED)
            System.out.printf("[ALU ADC] 0x%02X + 0x%02X +0x%02X = 0x%02X | Flags Generati: %s (Raw: 0x%02X)\n",
                    op1, op2, cy, result, getAluFlagsString(newFlags), newFlags);

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * SUB s / CP s -> Sottrae op2 da op1.
     * Se saveResult è true (SUB), trasmette il dato su SoCData.
     */
    public int sub(boolean saveResult) {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int op2 = regToAluBus2.sampleByte() & 0xFF;
        int result = (op1 - op2) & 0xFF;

        int newFlags = FLAG_N; // Sempre set nelle sottrazioni
        if (result == 0) newFlags |= FLAG_Z;
        if (((op1 & 0x0F) - (op2 & 0x0F)) < 0) newFlags |= FLAG_H;
        if ((op1 - op2) < 0) newFlags |= FLAG_C;

        if (saveResult) {
            soCData.broadcast(this, new ByteData(result));
        }
        return newFlags;
    }

    /**
     * SBC A, s -> Sottrae op2 e il Carry in ingresso da op1. Spedisce su SoCData.
     */
    public int sbc(boolean carryIn) {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int op2 = regToAluBus2.sampleByte() & 0xFF;
        int cy = carryIn ? 1 : 0;
        int result = (op1 - op2 - cy) & 0xFF;

        int newFlags = FLAG_N;
        if (result == 0) newFlags |= FLAG_Z;
        if (((op1 & 0x0F) - (op2 & 0x0F) - cy) < 0) newFlags |= FLAG_H;
        if ((op1 - op2 - cy) < 0) newFlags |= FLAG_C;

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * INC ss -> Incrementa op1 di 1. Preserva il flag Carry corrente. Spedisce su SoCData.
     */
    public int inc(int currentFlags) {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int result = (op1 + 1) & 0xFF;

        int newFlags = currentFlags & FLAG_C; // Il Carry non viene toccato da INC
        if (result == 0) newFlags |= FLAG_Z;
        if (((op1 & 0x0F) + 1) > 0x0F) newFlags |= FLAG_H;
        // FLAG_N è 0 (addizione)

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * DEC ss -> Decrementa op1 di 1. Preserva il flag Carry corrente. Spedisce su SoCData.
     */
    public int dec(int currentFlags) {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int result = (op1 - 1) & 0xFF;

        int newFlags = FLAG_N | (currentFlags & FLAG_C); // N fissato a 1, Carry preservato
        if (result == 0) newFlags |= FLAG_Z;
        if ((op1 & 0x0F) == 0) newFlags |= FLAG_H;

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    // ==========================================
    //            OPERAZIONI LOGICHE
    // ==========================================

    /**
     * AND s -> Bitwise AND tra op1 e op2. Spedisce su SoCData.
     */
    public int and() {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int op2 = regToAluBus2.sampleByte() & 0xFF;
        int result = op1 & op2;

        int newFlags = FLAG_H; // L'SM83 imposta sempre H a 1 nell'AND hardware!
        if (result == 0) newFlags |= FLAG_Z;
        // N e C vengono azzerati

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * OR s -> Bitwise OR tra op1 e op2. Spedisce su SoCData.
     */
    public int or() {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int op2 = regToAluBus2.sampleByte() & 0xFF;
        int result = op1 | op2;

        int newFlags = 0; // H, N, C azzerati in OR
        if (result == 0) newFlags |= FLAG_Z;

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * XOR s -> Bitwise XOR tra op1 e op2. Spedisce su SoCData.
     */
    public int xor() {
        int op1 = regToAluBus1.sampleByte() & 0xFF;
        int op2 = regToAluBus2.sampleByte() & 0xFF;
        int result = op1 ^ op2;

        int newFlags = 0; // H, N, C azzerati in XOR
        if (result == 0) newFlags |= FLAG_Z;

        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * CCF -> Complement Carry Flag.
     * Inverte il bit del Carry, azzera N,
     * copia il VECCHIO Carry in H, e preserva Z.
     */
    public int ccf(int currentFlags) {
        int newFlags = currentFlags & FLAG_Z;
        int oldCarry = currentFlags & FLAG_C;
        int invertedCarry = oldCarry ^ FLAG_C;
        int oldCarryMovedToH = oldCarry << 1;
        return newFlags | invertedCarry | oldCarryMovedToH;
    }

    /**
     * SCF -> Set Carry Flag.
     * Forza il bit del Carry a 1, azzera N e H, preserva Z.
     */
    public int scf(int currentFlags) {
        int newFlags = currentFlags & FLAG_Z;
        return newFlags | FLAG_C;
    }

    /**
     * DAA -> Decimal Adjust Accumulator.
     * Corregge il registro A per la codifica BCD dopo addizioni/sottrazioni.
     * Spedisce il risultato corretto su SoCData.
     */
    public int daa(int currentFlags) {
        int a = regToAluBus1.sampleByte() & 0xFF;

        boolean isSub = (currentFlags & FLAG_N) != 0;
        boolean hasH = (currentFlags & FLAG_H) != 0;
        boolean hasC = (currentFlags & FLAG_C) != 0;

        int correction = 0;
        boolean setCarry = false;

        if (!isSub) {
            if (hasH || (a & 0x0F) > 9) {
                correction |= 0x06;
            }
            if (hasC || a > 0x99) {
                correction |= 0x60;
                setCarry = true;
            }
            a = (a + correction) & 0xFF;
        } else {
            if (hasH) {
                correction |= 0x06;
            }
            if (hasC) {
                correction |= 0x60;
            }
            if (hasC) {
                setCarry = true;
            }
            a = (a - correction) & 0xFF;
        }

        int newFlags = currentFlags & FLAG_N;
        if (a == 0) newFlags |= FLAG_Z;
        if (setCarry) newFlags |= FLAG_C;

        soCData.broadcast(this, new ByteData(a));
        return newFlags;
    }

    /**
     * CPL -> Complement Accumulator.
     * Inverte bitwise il registro A (NOT). Forza N=1 e H=1, preserva Z e C.
     * Spedisce il risultato su SoCData.
     */
    public int cpl(int currentFlags) {
        int a = regToAluBus1.sampleByte() & 0xFF;
        int result = (~a) & 0xFF;
        int preservedFlags = currentFlags & (FLAG_Z | FLAG_C);
        int newFlags = preservedFlags | FLAG_N | FLAG_H;
        soCData.broadcast(this, new ByteData(result));
        return newFlags;
    }

    /**
     * RLA -> Ruota l'accumulatore a sinistra attraverso il Carry Flag.
     * Z, N, H vengono azzerati. C prende il vecchio Bit 7.
     */
    public int rla(int currentFlags) {
        int oldCarry = ((currentFlags & FLAG_C) != 0) ? 1 : 0;

        int currentA = regToAluBus1.sampleByte();
        int bit7 = (currentA >> 7) & 0x01;

        // Sposta a sinistra e inserisci il vecchio carry al bit 0
        int result = ((currentA << 1) | oldCarry) & 0xFF;

        // Invia il risultato sul SoCData bus se l'architettura lo richiede per il campionamento
        soCData.broadcast(this, new ByteData(result));

        // Flag dell'SM83 per RLA: Z=0, N=0, H=0, C=bit7
        int newFlags = 0;
        if (bit7 == 1) newFlags |= FLAG_C;

        return newFlags;
    }

    /**
     * RRA -> Ruota l'accumulatore a destra attraverso il Carry Flag.
     * Z, N, H vengono azzerati. C prende il vecchio Bit 0.
     */
    public int rra(int currentFlags) {
        int currentA = regToAluBus1.sampleByte();
        int oldCarry = ((currentFlags & FLAG_C) != 0) ? 1 : 0;
        int bit0 = currentA & 0x01;

        // Sposta a destra e inserisci il vecchio carry al bit 7 (0x80)
        int result = ((currentA >> 1) | (oldCarry << 7)) & 0xFF;

        // Invia il risultato sul SoCData bus
        soCData.broadcast(this, new ByteData(result));

        // Flag dell'SM83 per RRA: Z=0, N=0, H=0, C=bit0
        int newFlags = 0;
        if (bit0 == 1) newFlags |= FLAG_C;

        return newFlags;
    }


    private String getAluFlagsString(int f) {
        return String.format("[%s%s%s%s]",
                (f & FLAG_Z) != 0 ? "Z" : "-",
                (f & FLAG_N) != 0 ? "N" : "-",
                (f & FLAG_H) != 0 ? "H" : "-",
                (f & FLAG_C) != 0 ? "C" : "-"
        );
    }
    public int cpb() {
        int val1 = regToAluBus1.sampleByte() & 0xFF;
        int val2 = regToAluBus2.sampleByte() & 0xFF;
        int fullSum = val1 + val2;
        int lowNibbleSum = (val1 & 0x0F) + (val2 & 0x0F);
        int lastResult8Bit = fullSum & 0xFF;

        this.soCData.broadcast(this, new ByteData(lastResult8Bit));
        int flags = 0;
        if (lastResult8Bit == 0) {
            flags |= FLAG_Z;
        }
        if (lowNibbleSum > 0x0F) {
            flags |= FLAG_H;
        }
        if (fullSum > 0xFF) {
            flags |= FLAG_C;
        }
        if(Main.LOG_ALU_ENABLED)
            System.out.printf("[ALU CPB] 0x%02X + 0x%02X = 0x%02X | Flags Generati: %s (Raw: 0x%02X)\n", val1, val2, lastResult8Bit, getAluFlagsString(flags), flags);

        return flags;
    }
}