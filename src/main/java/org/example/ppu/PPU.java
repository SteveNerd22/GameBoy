package org.example.ppu;

import org.example.bus.*;
import org.example.bus.data.AddressData;
import org.example.bus.data.ByteData;
import org.example.bus.data.InterruptSignal;
import org.example.mmu.PhysicalMemory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class PPU implements BusWriter {
    public static final int SCREEN_WIDTH = 160;
    public static final int SCREEN_HEIGHT = 144;
    private final AddressBus addressBus;
    private final DataBus dataBus;
    private final InterruptBus interruptBus;
    private final PhysicalMemory vram, oam, ioRegisters;

    // --- Offset dei Registri Hardware dentro IO_REGISTERS ---
    private static final int REG_LCDC = 0x0040; // 0xFF40
    private static final int REG_STAT = 0x0041; // 0xFF41
    private static final int REG_SCY  = 0x0042; // 0xFF42
    private static final int REG_SCX  = 0x0043; // 0xFF43
    private static final int REG_LY   = 0x0044; // 0xFF44
    private static final int REG_LYC  = 0x0045; // 0xFF45
    private static final int REG_BGP  = 0x0047; // 0xFF47

    private int tCyclesCounter = 0;
    private final BufferedImage screenImage;
    private final int[] pixelRaster;

    private final int[] DMG_PALETTE = {
            0xE0F8D0, 0x88C070, 0x346856, 0x081820
    };

    public PPU(InterruptBus interruptBus, DataBus dataBus, AddressBus addressBus,
               PhysicalMemory vram, PhysicalMemory oam, PhysicalMemory ioRegisters) {
        this.interruptBus = interruptBus;
        this.dataBus = dataBus;
        this.addressBus = addressBus;
        this.vram = vram;
        this.oam = oam;
        this.ioRegisters = ioRegisters;

        this.screenImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
        this.pixelRaster = ((DataBufferInt) screenImage.getRaster().getDataBuffer()).getData();

        // Inizializzazione dei valori di boot hardware direttamente in memoria
        ioRegisters.write(this, REG_LCDC, 0x91);
        ioRegisters.write(this, REG_STAT, 0x85);
        ioRegisters.write(this, REG_BGP,  0xFC);

        clearScreen();
    }

    public void pulse() {
        // --- 1. AVANZAMENTO CONTATORI ---
        tCyclesCounter++;
        if (tCyclesCounter >= 456) {
            tCyclesCounter = 0;

            // Leggi il vecchio LY, incrementalo e riscrivilo direttamente nella memoria condivisa
            int currentLy = ioRegisters.read(this, REG_LY);
            currentLy++;
            if (currentLy > 153) currentLy = 0;

            ioRegisters.write(this, REG_LY, currentLy);
        }

        // Recuperiamo LY aggiornato per la macchina a stati interna
        int ly = ioRegisters.read(this, REG_LY);

        // --- 2. AGGIORNAMENTO MACCHINA A STATI ---
        int currentMode;
        if (ly >= 144) {
            currentMode = 1;
        } else if (tCyclesCounter < 80) {
            currentMode = 2;
        } else if (tCyclesCounter < 252) {
            currentMode = 3;
            if (tCyclesCounter == 80) {
                renderScanline();
            }
        } else {
            currentMode = 0;
        }

        // Aggiorna lo STAT register direttamente nella memoria condivisa
        int currentStat = ioRegisters.read(this, REG_STAT);
        currentStat = (currentStat & 0xFC) | currentMode;
        ioRegisters.write(this, REG_STAT, currentStat);

        // --- 3. CAMPIONAMENTO ATTIVO DEI BUS ---
        int address = addressBus.sampleAddress();

        // Ora la PPU intercetta SOLO la VRAM sul bus (perché ai registri IO ci pensa già la MMU!)
        if (address >= 0x8000 && address <= 0x9FFF) {
            if (currentMode != 3) {
                int byteData = dataBus.sampleByte();
                vram.write(this, address - 0x8000, byteData);
            }
        }
    }

    private void renderScanline() {
        int ly   = ioRegisters.read(this, REG_LY);
        int scy  = ioRegisters.read(this, REG_SCY);
        int scx  = ioRegisters.read(this, REG_SCX);

        int offset = ly * SCREEN_WIDTH;
        int worldY = (ly + scy) & 0xFF;
        int tileRow = worldY / 8;
        int pixelRowInTile = worldY % 8;
        int mapOffset = 0x1800;

        for (int x = 0; x < SCREEN_WIDTH; x++) {
            int worldX = (x + scx) & 0xFF;
            int tileCol = worldX / 8;
            int pixelColInTile = worldX % 8;

            int tileMapIndex = mapOffset + (tileRow * 32) + tileCol;
            int tileId = vram.read(this, tileMapIndex);

            int byteRowOffset = (tileId * 16) + (pixelRowInTile * 2);
            int byte1 = vram.read(this, byteRowOffset);
            int byte2 = vram.read(this, byteRowOffset + 1);

            int bitIndex = 7 - pixelColInTile;
            int bit1 = (byte1 >> bitIndex) & 0x01;
            int bit2 = (byte2 >> bitIndex) & 0x01;
            int colorId = (bit2 << 1) | bit1;

            pixelRaster[offset + x] = DMG_PALETTE[colorId];
        }
    }

    private void clearScreen() {
        for (int i = 0; i < pixelRaster.length; i++) {
            pixelRaster[i] = DMG_PALETTE[0];
        }
    }

    // Getter modificati per attingere dalla memoria reale
    public int getLy() { return ioRegisters.read(this, REG_LY); }
    public int getTCyclesCounter() { return this.tCyclesCounter; }
    public BufferedImage getScreenImage() { return screenImage; }
    public PhysicalMemory getVram() { return vram; }
}