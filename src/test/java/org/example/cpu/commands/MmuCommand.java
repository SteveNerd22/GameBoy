package org.example.cpu.commands;

import org.example.cpu.DebugCommand;
import org.example.cpu.DebugContext;
import org.example.mmu.MMU;

public class MmuCommand implements DebugCommand {
    @Override
    public String getKeyword() { return "mmu"; }

    @Override
    public String getHelp() { return "Ispeziona la MMU intorno a un indirizzo esadecimale (es: mmu C000)"; }

    @Override
    public boolean execute(DebugContext ctx, String[] args) {
        if (args.length < 1) {
            System.out.println("⚠️ Errore: Specifica un indirizzo. Uso: mmu [indirizzo_esadecimale]");
            return false;
        }

        try {
            // Puliamo la stringa da eventuali "0x" inseriti dall'utente per comodità
            String addrStr = args[0].replace("0x", "").trim();
            int targetAddr = Integer.parseInt(addrStr, 16);

            MMU mmu = ctx.getGameBoy().getMmu();

            System.out.printf("\n--- 💾 ISPEZIONE MMU INTORNO A 0x%04X ---\n", targetAddr);

            // Leggiamo la finestra di contesto (-4 byte fino a +4 byte)
            for (int i = -4; i <= 4; i++) {
                int currentAddress = (targetAddr + i) & 0xFFFF; // Maschera a 16-bit per evitare overflow/underflow
                String pointer = (i == 0) ? " => " : "    ";

                System.out.printf("%s[0x%04X] -> 0x%02X\n",
                        pointer,
                        currentAddress,
                        mmu.readByte(currentAddress, ctx.getCpu())
                );
            }
            System.out.println("-------------------------------------------\n");

        } catch (NumberFormatException e) {
            System.out.println("⚠️ Indirizzo non valido. Usa il formato esadecimale (es: mmu C000 o mmu 100)");
        } catch (Exception e) {
            System.out.println("⚠️ Errore durante la lettura della memoria: " + e.getMessage());
        }

        return false; // Comando ispettivo, non avanza il tempo
    }
}