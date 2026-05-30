package org.example.cpu;

// ... (i tuoi import rimangono uguali)

import org.example.bus.AddressBus;
import org.example.bus.Bus;
import org.example.bus.BusWriter;
import org.example.bus.DataBus;
import org.example.bus.data.AddressData;

public class PointerRegister extends Register {

    private final Bus<AddressData> soCAddress;
    private int value16 = 0x0000;

    public PointerRegister(DataBus soCData, AddressBus soCAddress, Bus<AddressData> privateIduBus) {
        super(soCData);
        this.soCAddress = soCAddress;
        soCAddress.registerWriter(this);

        if (privateIduBus != null) {
            privateIduBus.registerReader(this::onPrivateIduUpdate);
        }
    }

    @Override
    public int get() {
        return this.value16;
    }

    @Override
    public void setValue(int val) {
        this.value16 = val & 0xFFFF; // Maschera a 16-bit!
    }

    public void emitAddress() {
        soCAddress.broadcast(this, new AddressData(get()));
    }

    private void onPrivateIduUpdate(BusWriter sender, AddressData data) {
        setValue(data.getAddress());
    }
}