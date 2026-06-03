package org.example.cpu;

import org.example.Main;
import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.InterruptBus;
import org.example.mmu.MMU;

public class CpuTestRunner {
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    public static void run(CpuTestContext context) {
        // 1. Inizializzazione Hardware ad ogni test per garantire isolamento totale
        InterruptBus interruptBus = new InterruptBus();
        DataBus dataBus = new DataBus();
        AddressBus addressBus = new AddressBus();
        SM83 cpu = new SM83(interruptBus, dataBus, addressBus);
        MMU mmu = new MMU(addressBus, dataBus);

        // 2. Setup del Programma e reset dei puntatori
        mmu.loadCartridge(context.getProgram());
        cpu.PC.set(0x0000);

        // 3. Iniezione dello stato iniziale configurato nel contesto
        context.getInitialRegisters().forEach((reg, val) -> {
            switch (reg) {
                case "A" -> cpu.A.set(val);
                case "B" -> cpu.B.set(val);
                case "C" -> cpu.C.set(val);
                case "D" -> cpu.D.set(val);
                case "E" -> cpu.E.set(val);
                case "H" -> cpu.H.set(val);
                case "L" -> cpu.L.set(val);
                case "HL" -> cpu.HL.set(val);
            }
        });
        context.getInitialRam().forEach(mmu::writeByte);

        // 4. Avvio dell'Emulation Loop
        System.out.println("\n=== STARTING EMULATION LOOP ===");
        System.out.printf("Initial State -> PC: 0x%04X | A: 0x%02X | B: 0x%02X | HL: 0x%04X\n\n",
                cpu.PC.get(), cpu.A.get(), cpu.B.get(), cpu.HL.get());

        int tick = 0;
        int programEndAddress = context.getProgram().length;

        while (cpu.PC.get() < programEndAddress || tick % 4 != 0) {
            tick++;
            String macroState = cpu.getPipelineStatus();

            cpu.pulse();

            if (Main.DEBUG) {
                System.out.printf("  Tick %02d | Pipeline: %-7s | PC: 0x%04X | IR: 0x%02X | A: 0x%02X | B: 0x%02X\n",
                        tick, macroState, cpu.PC.get(), cpu.IR.get(), cpu.A.get(), cpu.B.get());
            }

            if (tick % 4 == 0) {
                int mCycle = tick / 4;
                System.out.printf("%s%s[M-CYCLE %02d]%s -> PC: 0x%04X | IR: 0x%02X | A: 0x%02X | B: 0x%02X | HL: 0x%04X\n",
                        BOLD, CYAN, mCycle, RESET, cpu.PC.get(), cpu.IR.get(), cpu.A.get(), cpu.B.get(), cpu.HL.get());
            }

            if (tick > 200) {
                System.out.println("\n[TIMEOUT] Forza arresto.");
                break;
            }
        }

        System.out.printf("\n=== TEST CONCLUSO IN %d TICK (%d M-CYCLES) ===\n", tick, tick / 4);

        // Stampiamo lo stato finale dei registri e della RAM utile per facilitare i controlli visivi
        System.out.printf("Final State   -> A: 0x%02X | B: 0x%02X | RAM[0xC000]: 0x%02X\n",
                cpu.A.get(), cpu.B.get(), mmu.readByte(0xC000));
        System.out.println("------------------------------------------------------------------------");
    }
}