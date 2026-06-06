package org.example.cpu.tests;

import org.example.GameBoy;
import org.example.Main;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.mmu.MMU;

public interface CpuTestCase {

    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Il punto di ingresso standard usato dal testSuiteRunner globale.
     */
    void execute(SM83 cpu, MMU mmu, TestReporter reporter);

    /**
     * METODO DEFAULT: Avvia il test in modalità standalone stampando la pipeline M-Cycle per M-Cycle.
     * Utilizzabile direttamente all'interno dei metodi main delle singole classi di test.
     */
    default void runAsPipelineTrace(int[] romData, int targetMCycles, java.util.function.Consumer<GameBoy> customSetup) {
        System.out.println("=======================================================================================");
        System.out.println("   STANDALONE PIPELINE TRACE: " + getName().toUpperCase());
        System.out.println("=======================================================================================");

        Main.DEBUG = true;

        // Istanziamo la scheda madre completa
        GameBoy gb = new GameBoy();
        SM83 cpu = gb.getCpu();
        MMU mmu = gb.getMmu();

        // Caricamento della ROM tramite la MMU del GameBoy
        int[] fakeCartridge = new int[0x4000];
        System.arraycopy(romData, 0, fakeCartridge, 0, Math.min(romData.length, fakeCartridge.length));
        gb.getMmu().loadCartridge(fakeCartridge);

        gb.reset();
        cpu.PC.set(0x0000);
        cpu.F.set(0x00);

        // Il customSetup ora ha accesso a TUTTO il GameBoy (CPU e MMU insieme!)
        if (customSetup != null) {
            customSetup.accept(gb);
        }

        System.out.println("Stato iniziale (Boot - Catena Vuota):");
        printStateLine(0, cpu, mmu, cpu.getExecutionEngine());
        System.out.println("---------------------------------------------------------------------------------------");

        for (int mCycle = 1; mCycle <= targetMCycles; mCycle++) {
            for (int tick = 0; tick < 4; tick++) {
                gb.pulse(); // Facciamo fare il pulse direttamente al sistema
            }
            printStateLine(mCycle, cpu, mmu, cpu.getExecutionEngine());
        }

        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("Fine della traccia della pipeline.");
        System.out.println("=======================================================================================\n");
    }

    /**
     * METODO DEFAULT INTERNO: Formatta e stampa i registri hardware e lo stato dell'Engine.
     */
    default void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-18s | B: 0x%02X | C: 0x%02X | D: 0x%02X | E: 0x%02X | A: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.B.get(),
                cpu.C.get(),
                cpu.D.get(),
                cpu.E.get(),
                cpu.A.get(),
                cpu.getTotalTicks()
        );
    }
}