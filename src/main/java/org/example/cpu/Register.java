package org.example.cpu;

import org.example.bus.BusReader;
import org.example.bus.BusWriter;
import org.example.bus.DataBus;
import org.example.bus.data.ByteData;

public class Register implements BusWriter {
    private int value = 0x00;
    private DataBus SoCBus;
    private DataBus aluBus1;
    private DataBus aluBus2;


    public Register(DataBus SoCBus, DataBus aluBus1, DataBus aluBus2) {
        this.SoCBus = SoCBus;
        this.aluBus1 = aluBus1;
        this.aluBus2 = aluBus2;
        if(SoCBus != null) {
            SoCBus.registerWriter(this);
        }
        if(aluBus1 != null) {
            aluBus1.registerWriter(this);
        }
        if(aluBus2 != null) {
            aluBus2.registerWriter(this);
        }
    }

    public Register(DataBus SoCBus) {
        this(SoCBus, null, null);
    }

    public Register(DataBus aluBus1, DataBus aluBus2) {
        this(null, aluBus1, aluBus2);
    }

    public int get() { return value; }
    public void setValue(int val) { this.value = val & 0xFF; }

    public void emit() {
        if(SoCBus != null)
            SoCBus.broadcast(this, new ByteData(this.value));
    }
}