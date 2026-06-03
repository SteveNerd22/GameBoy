package org.example.cpu;

import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.data.AddressData;

public class RegisterPair implements IRegister {
    private final Register high;
    private final Register low;
    private final AddressBus soCAddress;

    private final AddressBus iduToAddressRegisters;

    /**
     * Collega direttamente due celle di registro a 8-bit preesistenti per formare
     * un'unità funzionale a 16-bit, mappandola sulle rispettive linee dei bus di indirizzo.
     *
     * @param high                  Il registro a 8-bit destinato a ospitare la metà più significativa (MSB, bit 8-15).
     * @param low                   Il registro a 8-bit destinato a ospitare la metà meno significativa (LSB, bit 0-7).
     * @param soCAddress            L'Address Bus principale del sistema, utilizzato dalla coppia per fare il broadcast di indirizzi a 16-bit.
     * @param iduToAddressRegisters L'bus privato di ritorno dall'IDU (Incrementer/Decrementer Unit), usato per aggiornare al volo l'intera coppia.
     */
    public RegisterPair(Register high, Register low, AddressBus soCAddress, AddressBus iduToAddressRegisters) {
        this.high = high;
        this.low = low;
        this.soCAddress = soCAddress;
        this.iduToAddressRegisters = iduToAddressRegisters;
    }

    /**
     * Costruttore accoppiato per registri ad accesso ALU (es. coppia AF o BC).
     * Genera internamente due registri anonimi a 8-bit collegati al SoCDataBus, ai canali privati dell'ALU
     * e ai canali internalData per i passaggi interni.
     */
    public RegisterPair(DataBus SocDataBus, AddressBus SoCAddress, AddressBus iduToAddressRegisters,
                        DataBus aluBus1, DataBus aluBus2, DataBus internalData1, DataBus internalData2) {
        this (new Register(SocDataBus, aluBus1, aluBus2, internalData1), new Register(SocDataBus, aluBus1, aluBus2, internalData2),
                SoCAddress,  iduToAddressRegisters);
    }

    /**
     * Costruttore dedicato a SP e PC (Puntatori a 16-bit puri).
     * Genera due metà a 8-bit isolate elettricamente dall'ALU e dal bus interno. Non istanzia alcun pass-gate
     * verso i bus privati delle unità interne, poiché questi puntatori interagiscono
     * solo con il SoCDataBus (fase di fetch/stack) e con l'IDU per l'incremento/decremento.
     */
    public RegisterPair(DataBus SocDataBus, AddressBus SoCAddress, AddressBus iduToAddressRegisters) {
        this(new Register(SocDataBus, null, null,null), new Register(SocDataBus, null,null, null),
                SoCAddress, iduToAddressRegisters);
    }

    @Override
    public int get() {
        return ((this.high.get() & 0xFF) << 8) | (this.low.get() & 0xFF);
    }

    @Override
    public void set(int value) {
        this.high.set((value >> 8) & 0xFF);
        this.low.set(value & 0xFF);
    }

    @Override
    public void emit() {
        if(soCAddress != null)
            this.soCAddress.broadcast(this, new AddressData(get()));
    }

    public void sampleFromIduBus() {
        if (iduToAddressRegisters != null) {
            int address = iduToAddressRegisters.sampleAddress();
            this.high.set((address >> 8) & 0xFF);
            this.low.set(address & 0xFF);
        }
    }

    public Register getHigh() { return this.high; }
    public Register getLow() { return this.low; }
}