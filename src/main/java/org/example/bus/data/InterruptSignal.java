package org.example.bus.data;

public class InterruptSignal extends BusData {
    public static final int MEM_RD = 0x0100; // Bit 8 attivo
    public static final int MEM_WR = 0x0200; // Bit 9 attivo
    public static final int NONE   = 0x0000;
    private final int bitMask;

    public InterruptSignal(int bitMask) {
        this.bitMask = bitMask;
    }

    public int getBitMask() { return bitMask; }
}