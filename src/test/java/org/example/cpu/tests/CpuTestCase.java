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

    void execute(SM83 cpu, MMU mmu, TestReporter reporter);

    /**
     * Avvia il test in modalità standalone sfruttando l'architettura a Listener del GameBoy.
     */
    default void runAsPipelineTrace(int[] romData, int targetMCycles, java.util.function.Consumer<GameBoy> customSetup) {
        System.out.println("=======================================================================================");
        System.out.println("   STANDALONE PIPELINE TRACE (EVENT-DRIVEN): " + getName().toUpperCase());
        System.out.println("=======================================================================================");

        Main.DEBUG = true;

        // 1. Istanziamo la scheda madre
        GameBoy gb = new GameBoy();
        SM83 cpu = gb.getCpu();
        MMU mmu = gb.getMmu();

        // 2. Caricamento della ROM
        int[] fakeCartridge = new int[0x4000];
        System.arraycopy(romData, 0, fakeCartridge, 0, Math.min(romData.length, fakeCartridge.length));
        gb.getMmu().loadCartridge(fakeCartridge);

        gb.reset();
        cpu.PC.set(0x0000);
        cpu.F.set(0x00);

        if (customSetup != null) {
            customSetup.accept(gb);
        }

        System.out.println("Stato iniziale (Boot - Catena Vuota):");
        printStateLine(0, cpu, mmu, cpu.getExecutionEngine());
        System.out.println("---------------------------------------------------------------------------------------");

        // 3. REGISTRAZIONE DEL LISTENER
        // Usiamo un array di un elemento come trucco per aggirare la restrizione delle variabili final nelle lambda
        final int[] tickCounter = {0};
        final int[] mCycleCounter = {1};

        gb.RegisterPulseListener(() -> {
            tickCounter[0]++;

            // Ogni 4 T-Ticks (ovvero 1 Ciclo Macchina completo) stampiamo lo stato della pipeline
            if (tickCounter[0] >= 4) {
                tickCounter[0] = 0;
                printStateLine(mCycleCounter[0], cpu, mmu, cpu.getExecutionEngine());
                mCycleCounter[0]++;
            }
        });

        // 4. ESECUZIONE DEL TEST
        // Per simulare esattamente i cicli richiesti senza avviare il thread asincrono del clock,
        // possiamo forzare un loop controllato di pulse manuali se esponiamo un metodo di debug,
        // oppure far girare il clock per un numero fisso di passi.
        // Assumendo che per i test usiamo un loop di passaggi discreti:
        int totalRequiredTTicks = targetMCycles * 4;
        for (int t = 0; t < totalRequiredTTicks; t++) {
            // Nota: Se hai messo `pulseComponents` come protected, assicurati che
            // CpuTestCase sia nello stesso package o esponi un metodo pubblico 'pulseSingleTick()' in GameBoy
            gb.step();
        }

        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("Fine della traccia della pipeline.");
        System.out.println("=======================================================================================\n");
    }

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