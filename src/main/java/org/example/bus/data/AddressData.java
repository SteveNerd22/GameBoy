package org.example.bus.data;

public class AddressData extends BusData {
    private final int address;

    public AddressData(int address) {
        this.address = address & 0xFFFF; // Enforce 16-bit
    }

    public int getAddress() { return address; }
}