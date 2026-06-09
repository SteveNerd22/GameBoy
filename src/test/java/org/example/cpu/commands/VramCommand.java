package org.example.cpu.commands;

import org.example.GameBoy;
import org.example.cpu.DebugCommand;
import org.example.cpu.DebugContext;
import org.example.mmu.PhysicalMemory;

public class VramCommand implements DebugCommand {
    @Override
    public String getKeyword() { return "vram"; }

    @Override
    public String getHelp() { return "Ispeziona le celle della VRAM della PPU che contengono dati"; }

    @Override
    public boolean execute(DebugContext ctx, String[] args) {
        GameBoy gameBoy = ctx.getGameBoy();
        System.out.println("\n--- 📺 ISPEZIONE VRAM DELLA PPU (Solo byte diversi da 0) ---");
        int count = 0;

        // Recuperiamo la VRAM dalla PPU tramite il GameBoy presente nel contesto
        PhysicalMemory ppuVram = gameBoy.getPpu().getVram();

        for (int addr = 0x0000; addr < ppuVram.getSize(); addr++) {
            int value = ppuVram.read(gameBoy.getPpu(), addr);
            if (value != 0) {
                System.out.printf("VRAM Indirizzo Corrente [0x%04X] (Mappa: 0x%04X) -> Valore: 0x%02X\n",
                        addr, addr + 0x8000, value);
                count++;
            }
        }

        if (count == 0) {
            System.out.println("La VRAM è attualmente completamente vuota (tutti 0x00).");
        }
        System.out.println("-----------------------------------------------------------\n");

        return false; // Comando ispettivo, non avanza il tempo
    }
}