package org.example.mmu;

import org.example.bus.*;
import org.example.bus.data.AddressData;
import org.example.bus.data.BusData;
import org.example.bus.data.ByteData;
import org.example.bus.data.InterruptSignal;

public class MMU implements BusWriter {

    // Memoria fisica interna (struttura Enum precedente)
    private final int[] vram = new int[MemoryRegion.VRAM.getEnd() - MemoryRegion.VRAM.getStart() + 1];
    private final int[] wram = new int[MemoryRegion.WRAM.getEnd() - MemoryRegion.WRAM.getStart() + 1];
    private final int[] oam  = new int[MemoryRegion.OAM.getEnd() - MemoryRegion.OAM.getStart() + 1];
    private final int[] hram = new int[MemoryRegion.HRAM.getEnd() - MemoryRegion.HRAM.getStart() + 1];

    private int interruptEnable = 0x00;
    private int interruptFlags = 0x00;
    private int[] cartridgeRom;

    // Riferimenti ai Bus per campionamento attivo o risposte immediate
    private final AddressBus addressBus;
    private final DataBus dataBus;
    private final InterruptBus interruptBus;
    private int currentControlSignal = InterruptSignal.NONE;

    // Stato interno temporaneo: l'ultimo indirizzo apparso sulle piste del bus
    private int latchedAddress = 0x0000;

    public MMU(AddressBus addressBus, DataBus dataBus, InterruptBus interruptBus) {
        this.addressBus = addressBus;
        this.dataBus = dataBus;
        this.interruptBus = interruptBus;

        this.interruptBus.registerReader((_, signal) -> this.currentControlSignal = signal.getBitMask());

        this.addressBus.registerReader((_, data) -> {
            this.latchedAddress = data.getAddress() & 0xFFFF;
            executeMemoryAccess();
        });

        this.dataBus.registerWriter(this);
    }

    private void executeMemoryAccess() {
        if ((this.currentControlSignal & InterruptSignal.MEM_RD) != 0) {
            int byteRead = readPhysicalMemory(this.latchedAddress);
            this.dataBus.broadcast(this, new ByteData(byteRead));
        }

        else if ((this.currentControlSignal & InterruptSignal.MEM_WR) != 0) {
            int valueToWrite = this.dataBus.sampleByte();
            writePhysicalMemory(this.latchedAddress, valueToWrite);
        }
    }

    public void loadCartridge(int[] romData) {
        this.cartridgeRom = romData;
    }

    /**
     * METODO PER LETTURA ATTIVA (Se altri componenti hanno il riferimento all'MMU e vogliono fare un bypass)
     */
    public int readByte(int address) {
        return readPhysicalMemory(address & 0xFFFF);
    }

    /**
     * METODO PER SCRITTURA ATTIVA / GESTIONE EVENTO SCRITTURA BUS
     * Chiamato quando la CPU vuole forzare una scrittura di un byte in memoria.
     */
    public void writeByte(int address, int value) {
        writePhysicalMemory(address & 0xFFFF, value & 0xFF);
    }

    // =========================================================================
    // LOGICA DI CONTROLLO INTERNA DEI CHIP (Sfoltita e pulita)
    // =========================================================================

    private int readPhysicalMemory(int address) {
        MemoryRegion region = MemoryRegion.of(address);

        return switch (region) {
            case ROM_BANK_0, ROM_BANK_1_N -> {
                if (cartridgeRom == null || address >= cartridgeRom.length) yield 0xFF;
                yield cartridgeRom[address];
            }
            case VRAM -> vram[address - MemoryRegion.VRAM.getStart()];
            case EXTERNAL_RAM -> 0xFF;
            case WRAM -> wram[address - MemoryRegion.WRAM.getStart()];
            case ECHO_RAM -> wram[address - MemoryRegion.ECHO_RAM.getStart()];
            case OAM -> oam[address - MemoryRegion.OAM.getStart()];
            case NOT_USABLE -> 0xFF;
            case IO_REGISTERS -> (address == 0xFF0F) ? interruptFlags : 0x00; // TODO: agganciare Joypad/PPU
            case HRAM -> hram[address - MemoryRegion.HRAM.getStart()];
            case INTERRUPT_ENABLE -> interruptEnable;
        };
    }

    private void writePhysicalMemory(int address, int value) {
        MemoryRegion region = MemoryRegion.of(address);

        switch (region) {
            case ROM_BANK_0, ROM_BANK_1_N, EXTERNAL_RAM, NOT_USABLE -> {}
            case VRAM -> vram[address - MemoryRegion.VRAM.getStart()] = value;
            case WRAM -> wram[address - MemoryRegion.WRAM.getStart()] = value;
            case ECHO_RAM -> wram[address - MemoryRegion.ECHO_RAM.getStart()] = value;
            case OAM -> oam[address - MemoryRegion.OAM.getStart()] = value;
            case IO_REGISTERS -> {
                if (address == 0xFF0F) interruptFlags = value;
            }
            case HRAM -> hram[address - MemoryRegion.HRAM.getStart()] = value;
            case INTERRUPT_ENABLE -> interruptEnable = value;
        }
    }

    /**
     * Canale preferenziale hardware privato per la PPU (Bypass dei Bus)
     */
    public int ppuReadVram(int address) {
        return vram[address - MemoryRegion.VRAM.getStart()];
    }
}