package org.example.cpu;

import org.example.bus.DataBus;
import org.example.bus.data.ByteData;

public class Register implements IRegister {
    private int value;
    private final DataBus soCData;
    private final DataBus aluBus;
    private final DataBus internalData; // Il nuovo canale per la comunicazione inter-registro

    public Register(DataBus soCData, DataBus aluBus, DataBus internalData) {
        this.soCData = soCData;
        this.aluBus = aluBus;
        this.internalData = internalData;
    }

    @Override
    public int get() {
        return this.value & 0xFF;
    }

    @Override
    public void set(int value) {
        this.value = value & 0xFF;
    }

    @Override
    public void emit() {
        if(soCData != null)
            this.soCData.broadcast(this, new ByteData(get()));
    }

    public void emitToAluBus() {
        if(aluBus != null)
            this.aluBus.broadcast(this, new ByteData(get()));
    }

    /**
     * Attiva i pass-gate interni per presentare il valore sul bus dati privato della CPU.
     * Usato per i trasferimenti inter-registro (es. LD B, A).
     */
    public void emitToInternalData() {
        if(internalData != null)
            this.internalData.broadcast(this, new ByteData(get()));
    }

    public int sampleSoCBus() {
        if(soCData != null)
            set(soCData.sampleByte());
        return get();
    }

    /**
     * Il registro apre le sue porte logiche e campiona il byte presente sul bus interno della CPU.
     */
    public int sampleInternalData() {
        if(internalData != null)
            set(internalData.sampleByte());
        return get();
    }
}