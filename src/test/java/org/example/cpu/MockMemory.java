package org.example.cpu;

import org.example.bus.AddressBus;
import org.example.bus.BusReader;
import org.example.bus.BusWriter;
import org.example.bus.DataBus;
import org.example.bus.data.AddressData;
import org.example.bus.data.ByteData;

public class MockMemory implements BusReader<AddressData>, BusWriter {

    private final DataBus dataBus;
    private final int[] memorySpace = new int[0x10000]; // 64KB di indirizzamento piatto

    public MockMemory(AddressBus addressBus, DataBus dataBus) {
        this.dataBus = dataBus;
        addressBus.registerReader(this);
    }

    public void loadProgram(int startAddress, int[] bytecode) {
        for (int i = 0; i < bytecode.length; i++) {
            this.memorySpace[startAddress + i] = bytecode[i] & 0xFF;
        }
    }

    @Override
    public void onBusWrite(BusWriter sender, AddressData data) {
        int requestedAddress = data.getAddress();
        int byteValue = memorySpace[requestedAddress];
        dataBus.broadcast(this, new ByteData(byteValue));
    }
}