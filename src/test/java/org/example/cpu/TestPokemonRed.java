package org.example.cpu;

import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.InterruptBus;
import org.example.mmu.MMU;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestPokemonRed {

    public static void main(String[] args) {
        Path romPath = Paths.get("PokemonRed.gb");

        if (!Files.exists(romPath)) {
            System.err.println("❌ ERRORE: ROM di Pokemon Rosso non trovata in: " + romPath.toAbsolutePath());
            return;
        }

        try {
            // Leggiamo la ROM e convertiamo in int[] per la tua MMU
            byte[] rawBytes = Files.readAllBytes(romPath);
            int[] romData = new int[rawBytes.length];
            for (int i = 0; i < rawBytes.length; i++) {
                romData[i] = rawBytes[i] & 0xFF;
            }

            System.out.println("💾 ROM Caricata! Dimensione: " + romData.length + " bytes.");
            System.out.println("🚀 Inizializzazione Hardware dell'emulatore...");

            // --- 2. SETUP HARDWARE ---

            DataBus dataBus = new DataBus();
            AddressBus addressBus = new AddressBus();
            InterruptBus interruptBus = new InterruptBus();

            MMU mmu = new MMU(addressBus, dataBus, interruptBus);
            mmu.loadCartridge(romData);

            SM83 cpu = new SM83(interruptBus, dataBus, addressBus);
            cpu.reset();

            cpu.PC.set(0x0100);
            cpu.SP.set(0xFFFE);
            cpu.A.set(0x01);
            cpu.F.set(0xB0);
            cpu.B.set(0x00);
            cpu.C.set(0x13);
            cpu.D.set(0x00);
            cpu.E.set(0xD8);
            cpu.H.set(0x01);
            cpu.L.set(0x4D);

            System.out.println("🔥 Sistema pronto. PC impostato a 0x0100. VIA AL MASSACRO!");
            System.out.println("=======================================================================================");

            long mCyclesExecuted = 0;
            boolean running = true;

            // --- 4. LOOP DI EMULAZIONE CIECO ---
            while (running) {
                try {
                    // Stampiamo lo stato ATTUALE prima del ciclo macchina
                    int currentPC = cpu.PC.get();
                    int opcode = mmu.readByte(currentPC); // Vediamo cosa sta per leggere

                    // Un ciclo macchina completo (4 impulsi di clock T-Ticks)
                    for (int i = 0; i < 4; i++) {
                        cpu.pulse();
                    }

                    mCyclesExecuted++;

                    // Logga il progresso sui bus
                    System.out.printf(
                            "M-Cycle: %-5d | PC: 0x%04X | Opcode letto: 0x%02X | IR: 0x%02X | A: 0x%02X | HL: 0x%04X | SP: 0x%04X | Flags: %s\n",
                            mCyclesExecuted,
                            currentPC,
                            opcode,
                            cpu.IR.get(),
                            cpu.A.get(),
                            cpu.HL.get(),
                            cpu.SP.get(),
                            getFlagsString(cpu.F.get())
                    );

                } catch (IllegalStateException e) {
                    System.out.println("\n💥 CRASH! La CPU ha sollevato un'eccezione logica:");
                    System.out.println("👉 " + e.getMessage());
                    running = false;
                } catch (Exception e) {
                    System.out.println("\n💥 CRASH HARDWARE GENERICO!");
                    e.printStackTrace();
                    running = false;
                }
            }

            System.out.println("=======================================================================================");
            System.out.printf("Fine Simulazione. Cicli Macchina eseguiti prima del botto: %d\n", mCyclesExecuted);

        } catch (IOException e) {
            System.err.println("Errore di lettura del file: " + e.getMessage());
        }
    }

    private static String getFlagsString(int f) {
        return String.format("[%s%s%s%s]",
                (f & 0x80) != 0 ? "Z" : "-",
                (f & 0x40) != 0 ? "N" : "-",
                (f & 0x20) != 0 ? "H" : "-",
                (f & 0x10) != 0 ? "C" : "-"
        );
    }
}
