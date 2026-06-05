package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdSpHl implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xF9, // M1-M2: LD SP, HL -> Copia HL dentro SP (Richiede 2 M-Cycles)
                0x00, // M3:    NOP (Overlap)
                0x00  // M4:    NOP
        };

        new Test_LdSpHl().runAsPipelineTrace(rom, 4, gb -> {
            // Setup iniziale: carichiamo un valore in HL da travasare in SP
            gb.getCpu().H.set(0xC1);
            gb.getCpu().L.set(0x00);

            // Stack pointer inizialmente azzerato
            gb.getCpu().SP.set(0x0000);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xF9, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.H.set(0xC1);
        cpu.L.set(0x00);
        cpu.SP.set(0x0000);

        // 8 T-Ticks = 2 M-Cycles totali per completare l'intera operazione
        for (int i = 0; i < 8; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA CPU ---
        reporter.incrementAssertions();
        if (cpu.SP.get() != 0xC100) {
            reporter.reportFailure(0xF9, String.format(
                    "LD SP, HL fallito: Il registro SP contiene 0x%04X, atteso: 0xC100", cpu.SP.get()
            ));
        }
    }

    /**
     * Telemetria per osservare il comportamento dei registri durante il ciclo interno
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-20s | HL: 0x%04X | SP: 0x%04X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.HL.get(),
                cpu.SP.get(),
                cpu.getTotalTicks()
        );
    }
}