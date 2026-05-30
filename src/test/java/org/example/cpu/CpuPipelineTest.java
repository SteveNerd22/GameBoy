package org.example.cpu;

import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.InterruptBus;
import org.example.mmu.MMU; // Importiamo il nostro nuovo centralino

public class CpuPipelineTest {
    public static void main(String[] args) {
        System.out.println("=== INITIALIZING SoC BUSES ===");
        InterruptBus interruptBus = new InterruptBus();
        DataBus dataBus = new DataBus();
        AddressBus addressBus = new AddressBus();

        System.out.println("=== CREATING HARDWARE ===");
        // La CPU rimane identica, legata ai suoi bus
        SM83 cpu = new SM83(interruptBus, dataBus, addressBus);

        // Sostituiamo il MockMemory con la MMU ufficiale legata ai bus
        MMU mmu = new MMU(addressBus, dataBus);

        // Il nostro programma di test (NOP, LD A, 0x42, LD B, A)
        int[] program = {
                0x00,       // NOP
                0x3E, 0x42, // LD A, 0x42
                0x47        // LD B, A
        };

        // Carichiamo il programma direttamente nella ROM della cartuccia virtuale
        mmu.loadCartridge(program);

        // Puntiamo il Program Counter all'inizio della ROM
        cpu.PC.setValue(0x0000);

        System.out.println("\n=== STARTING EMULATION LOOP ===");
        System.out.printf("Initial State -> PC: 0x%04X, A: 0x%02X, B: 0x%02X\n\n", cpu.PC.get(), cpu.A.get(), cpu.B.get());

        for (int tick = 1; tick <= 10; tick++) {
            String macroState = cpu.getPipelineStatus();

            // L'oscillatore virtuale batte il tempo sulla CPU.
            // La MMU risponderà passivamente tramite le callback dei bus!
            cpu.pulse();

            System.out.printf("Tick %02d | Pipeline: %-7s | PC: 0x%04X | A: 0x%02X | B: 0x%02X\n",
                    tick,
                    macroState,
                    cpu.PC.get(),
                    cpu.A.get(),
                    cpu.B.get()
            );
        }
    }
}