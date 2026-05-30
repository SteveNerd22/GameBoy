package org.example.bus;

import org.example.bus.data.AddressData;

public class AddressBus extends Bus<AddressData> {

    @Override
    protected void send(BusWriter sender, AddressData data, BusReader<AddressData> reader) {
        reader.onBusWrite(sender, data);
    }

    @Override
    public void onBusWrite(BusWriter sender, AddressData data) {
        // If the bus itself receives a write event, it forwards it to everyone
        broadcast(sender, data);
    }

    public int sampleAddress() {
        return (sample() != null) ? sample().getAddress() : 0x0000;
    }
}