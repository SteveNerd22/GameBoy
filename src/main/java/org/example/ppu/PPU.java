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

    PhysicalMemory vram, oam;


    // --- Registri Interni ---
    private int lcdc = 0x91;
    private int stat = 0x85;
    private int scy  = 0x00;
    private int scx  = 0x00;
    private int ly   = 0x00;
    private int lyc  = 0x00;
    private int bgp  = 0xFC;

    private int tCyclesCounter = 0;
    private final BufferedImage screenImage;
    private final int[] pixelRaster;

    private final int[] DMG_PALETTE = {
            0xE0F8D0, 0x88C070, 0x346856, 0x081820
    };

    public PPU(InterruptBus interruptBus, DataBus dataBus, AddressBus addressBus, PhysicalMemory vram, PhysicalMemory oam) {
        this.interruptBus = interruptBus;
        this.dataBus = dataBus;
        this.addressBus = addressBus;
        this.screenImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
        this.pixelRaster = ((DataBufferInt) screenImage.getRaster().getDataBuffer()).getData();
        this.interruptBus.registerWriter(this);
        this.vram = vram;
        this.oam = oam;
        clearScreen();
    }

    public void pulse() {
        // --- 1. AVANZAMENTO CONTATORI ---
        tCyclesCounter++;
        if (tCyclesCounter >= 456) {
            tCyclesCounter = 0;
            ly++;
            if (ly > 153) ly = 0;
        }

        // --- 2. AGGIORNAMENTO MACCHINA A STATI (Esempio semplificato) ---
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
        stat = (stat & 0xFC) | currentMode;

        // --- 3. CAMPIONAMENTO ATTIVO DEI BUS ---
        int address = addressBus.sampleAddress();

        if (address >= 0x8000 && address <= 0x9FFF) {
            if (currentMode != 3) {
                int byteData = dataBus.sampleByte();
                vram.write(this, address - 0x8000, byteData);
            }
        } else if (address >= 0xFF40 && address <= 0xFF47) {
            handleRegisterAccess(address);
        }

    }

    private void handleRegisterAccess(int address) {
        // 1. Campioniamo il segnale di controllo dall'InterruptBus per capire l'intenzione della CPU
        int ctrlSignal = interruptBus.sampleSignal();

        // --- CASO SCRITTURA (Bit 9 attivo: MEM_WR -> 0x0200) ---
        if ((ctrlSignal & InterruptSignal.MEM_WR) != 0) {
            int byteData = dataBus.sampleByte();
            switch (address) {
                case 0xFF40 -> lcdc = byteData;
                case 0xFF41 -> stat = (stat & 0x87) | (byteData & 0x78); // Cambiano solo i bit 3-6
                case 0xFF42 -> scy = byteData;
                case 0xFF43 -> scx = byteData;
                case 0xFF45 -> lyc = byteData;
                case 0xFF47 -> bgp = byteData;
            }
        }
        // --- CASO LETTURA (Bit 8 attivo: MEM_RD -> 0x0100) ---
        else if ((ctrlSignal & InterruptSignal.MEM_RD) != 0) {
            int value = switch (address) {
                case 0xFF40 -> lcdc;
                case 0xFF41 -> stat;
                case 0xFF42 -> scy;
                case 0xFF43 -> scx;
                case 0xFF44 -> ly;   // La CPU legge la scanline attuale
                case 0xFF45 -> lyc;
                case 0xFF47 -> bgp;
                default -> 0xFF;
            };
            dataBus.broadcast(this, new ByteData(value));
        }
    }

    private void renderScanline() {
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

    public int getLy() { return this.ly; }
    public int getTCyclesCounter() { return this.tCyclesCounter; }
    public BufferedImage getScreenImage() { return screenImage; }

    public PhysicalMemory getVram() {
        return vram;
    }
}