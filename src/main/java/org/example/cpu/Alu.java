package org.example.cpu;

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
}