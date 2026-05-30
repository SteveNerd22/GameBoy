package org.example.cpu;

import org.example.bus.Bus;
import org.example.bus.BusReader;
import org.example.bus.BusWriter;
import org.example.bus.data.AddressData;

public class Idu implements BusWriter, BusReader<AddressData> {

    private final Bus<AddressData> privateIduBus;
    private final Bus<AddressData> SoCAddressBus;

    public Idu(Bus<AddressData> SoCAddressBus, Bus<AddressData> privateIduBus) {
        this.privateIduBus = privateIduBus;
        this.SoCAddressBus = SoCAddressBus;
        privateIduBus.registerWriter(this);
    }

    /**
     * Takes a value, increments it, and broadcasts it on the private bus
     * so the target PointerRegister can update itself instantly.
     */
    public void increment(int currentAddress) {
        int nextAddress = (currentAddress + 1) & 0xFFFF;
        privateIduBus.broadcast(this, new AddressData(nextAddress));
    }

    @Override
    public void onBusWrite(BusWriter sender, AddressData data) {
        // TODO: fare qualcosa qui, non so ancora cosa, si vedrà
    }
}