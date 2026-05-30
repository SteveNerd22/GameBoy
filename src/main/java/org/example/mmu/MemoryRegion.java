package org.example.mmu;

public enum MemoryRegion {
    ROM_BANK_0(0x0000, 0x3FFF),
    ROM_BANK_1_N(0x4000, 0x7FFF),
    VRAM(0x8000, 0x9FFF),
    EXTERNAL_RAM(0xA000, 0xBFFF),
    WRAM(0xC000, 0xDFFF),
    ECHO_RAM(0xE000, 0xFDFF),
    OAM(0xFE00, 0xFE9F),
    NOT_USABLE(0xFEA0, 0xFEFF),
    IO_REGISTERS(0xFF00, 0xFF7F),
    HRAM(0xFF80, 0xFFFE),
    INTERRUPT_ENABLE(0xFFFF, 0xFFFF);

    private final int startAddress;
    private final int endAddress;

    MemoryRegion(int startAddress, int endAddress) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }

    public int getStart() {
        return startAddress;
    }

    public int getEnd() {
        return endAddress;
    }

    /**
     * Verifica se un determinato indirizzo a 16-bit appartiene a questa regione.
     */
    public boolean contains(int address) {
        return address >= this.startAddress && address <= this.endAddress;
    }

    /**
     * Trova dinamicamente la regione corrispondente a un indirizzo.
     */
    public static MemoryRegion of(int address) {
        for (MemoryRegion region : values()) {
            if (region.contains(address)) {
                return region;
            }
        }
        // Fallback teorico se l'indirizzo esce fuori dai 16-bit (impossibile con il mascheramento)
        return NOT_USABLE;
    }
}