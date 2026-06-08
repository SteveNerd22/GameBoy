package org.example.mmu;

import org.example.bus.*;
import org.example.bus.data.ByteData;
import org.example.bus.data.InterruptSignal;

public class MMU implements BusWriter {

    // Le memorie fisiche reali vengono create internamente
    private final PhysicalMemory romBank0;
    private final PhysicalMemory romBank1N;
    private final PhysicalMemory vram;
    private final PhysicalMemory externalRam;
    private final PhysicalMemory wram;
    private final PhysicalMemory oam;
    private final PhysicalMemory ioRegisters;
    private final PhysicalMemory hram;
    private final PhysicalMemory interruptEnable;

    // Bus di comunicazione
    private final AddressBus addressBus;
    private final DataBus dataBus;
    private final InterruptBus interruptBus;
    private int currentControlSignal = InterruptSignal.NONE;
    private int latchedAddress = 0x0000;

    public MMU(AddressBus addressBus, DataBus dataBus, InterruptBus interruptBus) {
        this.addressBus = addressBus;
        this.dataBus = dataBus;
        this.interruptBus = interruptBus;

        // Inizializzazione automatica sfruttando le dimensioni calcolate dall'Enum MemoryRegion
        this.romBank0       = new PhysicalMemory(getRegionSize(MemoryRegion.ROM_BANK_0));
        this.romBank1N      = new PhysicalMemory(getRegionSize(MemoryRegion.ROM_BANK_1_N));
        this.vram           = new PhysicalMemory(getRegionSize(MemoryRegion.VRAM));
        this.externalRam    = new PhysicalMemory(getRegionSize(MemoryRegion.EXTERNAL_RAM));
        this.wram           = new PhysicalMemory(getRegionSize(MemoryRegion.WRAM));
        this.oam            = new PhysicalMemory(getRegionSize(MemoryRegion.OAM));
        this.ioRegisters    = new PhysicalMemory(getRegionSize(MemoryRegion.IO_REGISTERS));
        this.hram           = new PhysicalMemory(getRegionSize(MemoryRegion.HRAM));
        this.interruptEnable = new PhysicalMemory(getRegionSize(MemoryRegion.INTERRUPT_ENABLE));

        // Listener dei bus (Invariati)
        this.interruptBus.registerReader((_, signal) -> this.currentControlSignal = signal.getBitMask());
        this.addressBus.registerReader((requestor, data) -> {
            this.latchedAddress = data.getAddress() & 0xFFFF;
            executeMemoryAccess(requestor);
        });
        this.dataBus.registerWriter(this);
    }

    private void executeMemoryAccess(BusWriter requestor) {

        if ((this.currentControlSignal & InterruptSignal.MEM_RD) != 0) {
            int byteRead = readPhysicalMemory(this.latchedAddress, requestor);
            this.dataBus.broadcast(this, new ByteData(byteRead));
        }
        if ((this.currentControlSignal & InterruptSignal.MEM_WR) != 0) {
            writePhysicalMemory(latchedAddress, dataBus.sampleByte(), requestor);
        }
    }

    private int getRegionSize(MemoryRegion region) {
        return region.getEnd() - region.getStart() + 1;
    }

    // =========================================================================
    // GETTER PER I COMPONENTI ESTERNI (PPU, APU, DMA...)
    // =========================================================================

    public PhysicalMemory getVram() { return vram; }
    public PhysicalMemory getOam() { return oam; }
    public PhysicalMemory getIoRegisters() { return ioRegisters; }

    public void loadCartridge(int[] romData) {
        for (int i = 0; i < romData.length; i++) {
            if (i < romBank0.getSize()) {
                romBank0.write(this, i, romData[i]);
            } else if (i < (romBank0.getSize() + romBank1N.getSize())) {
                romBank1N.write(this, i - romBank0.getSize(), romData[i]);
            }
        }
    }

    // =========================================================================
    // LOGICA DI ROUTING (Invariata, usa i riferimenti interni)
    // =========================================================================

    private int readPhysicalMemory(int address, BusWriter requestor) {
        MemoryRegion region = MemoryRegion.of(address);

        return switch (region) {
            case ROM_BANK_0     -> romBank0.read(requestor, address - MemoryRegion.ROM_BANK_0.getStart());
            case ROM_BANK_1_N   -> romBank1N.read(requestor, address - MemoryRegion.ROM_BANK_1_N.getStart());
            case VRAM           -> vram.read(requestor, address - MemoryRegion.VRAM.getStart());
            case EXTERNAL_RAM   -> externalRam.read(requestor, address - MemoryRegion.EXTERNAL_RAM.getStart());
            case WRAM           -> wram.read(requestor, address - MemoryRegion.WRAM.getStart());
            case ECHO_RAM       -> wram.read(requestor, address - MemoryRegion.ECHO_RAM.getStart());
            case OAM            -> oam.read(requestor, address - MemoryRegion.OAM.getStart());
            case NOT_USABLE     -> 0xFF;
            case IO_REGISTERS   -> ioRegisters.read(requestor, address - MemoryRegion.IO_REGISTERS.getStart());
            case HRAM           -> hram.read(requestor, address - MemoryRegion.HRAM.getStart());
            case INTERRUPT_ENABLE -> interruptEnable.read(requestor, address - MemoryRegion.INTERRUPT_ENABLE.getStart());
        };
    }

    private void writePhysicalMemory(int address, int value, BusWriter requestor) {
        MemoryRegion region = MemoryRegion.of(address);

        switch (region) {
            case ROM_BANK_0, ROM_BANK_1_N -> {}
            case VRAM           -> vram.write(requestor, address - MemoryRegion.VRAM.getStart(), value);
            case EXTERNAL_RAM   -> externalRam.write(requestor, address - MemoryRegion.EXTERNAL_RAM.getStart(), value);
            case WRAM           -> wram.write(requestor, address - MemoryRegion.WRAM.getStart(), value);
            case ECHO_RAM       -> wram.write(requestor, address - MemoryRegion.ECHO_RAM.getStart(), value);
            case OAM            -> oam.write(requestor, address - MemoryRegion.OAM.getStart(), value);
            case NOT_USABLE     -> {}
            case IO_REGISTERS   -> ioRegisters.write(requestor, address - MemoryRegion.IO_REGISTERS.getStart(), value);
            case HRAM           -> hram.write(requestor, address - MemoryRegion.HRAM.getStart(), value);
            case INTERRUPT_ENABLE -> interruptEnable.write(requestor, address - MemoryRegion.INTERRUPT_ENABLE.getStart(), value);
        }
    }

    public int readByte(int address, BusWriter requestor) { return readPhysicalMemory(address & 0xFFFF, requestor); }
    public void writeByte(int address, int value, BusWriter requestor) { writePhysicalMemory(address & 0xFFFF, value & 0xFF, requestor); }
}