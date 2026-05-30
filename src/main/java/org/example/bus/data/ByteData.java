package org.example.bus.data;

public class ByteData extends BusData {
    private final int byteValue;

    public ByteData(int byteValue) {
        this.byteValue = byteValue & 0xFF; // Enforce 8-bit
    }

    public int getByteValue() { return byteValue; }
}