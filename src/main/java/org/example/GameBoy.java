package org.example;

import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.InterruptBus;
import org.example.cpu.SM83;
import org.example.mmu.MMU;

public class GameBoy {

    private final AddressBus addressBus;
    private final DataBus dataBus;
    private final InterruptBus interruptBus;
    private final MMU mmu;
    private final SM83 cpu;

    public GameBoy() {
        // 1. Inizializzazione dei Bus di comunicazione hardware
        this.addressBus = new AddressBus();
        this.dataBus = new DataBus();
        this.interruptBus = new InterruptBus();

        // 2. Inizializzazione della Memoria collegata ai Bus
        this.mmu = new MMU(this.addressBus, this.dataBus);

        // 3. Inizializzazione della CPU (le passiamo i bus con l'ordine richiesto dal tuo costruttore)
        this.cpu = new SM83(this.interruptBus, this.dataBus, this.addressBus);
    }

    /**
     * Esegue un singolo impulso di clock (1 T-Tick) sull'intero sistema.
     */
    public void pulse() {
        this.cpu.pulse();
        // In futuro qui chiameremo anche: ppu.pulse(), timer.pulse(), ecc.
    }

    public SM83 getCpu() {
        return this.cpu;
    }

    public MMU getMmu() {
        return this.mmu;
    }

    /**
     * Reset hardware coordinato di tutti i componenti del Game Boy.
     */
    public void reset() {
        this.cpu.reset();
    }
}