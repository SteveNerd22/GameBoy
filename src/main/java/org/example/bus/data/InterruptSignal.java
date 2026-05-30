package org.example.bus.data;

public class InterruptSignal extends BusData {
    private final int bitMask;

    public InterruptSignal(int bitMask) {
        this.bitMask = bitMask;
    }

    public int getBitMask() { return bitMask; }
}