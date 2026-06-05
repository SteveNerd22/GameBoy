package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdRegisterPairImmediate16 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x31, 0xFE, 0xFF, // M1-M3: LD SP, 0xFFFE (Inizializzazione tipica dello Stack)
                0x00,             // M4:    NOP (Overlap)
                0x00              // M5:    NOP
        };

        new Test_LdRegisterPairImmediate16().runAsPipelineTrace(rom, 5, gb -> {
            // Azzariamo lo Stack Pointer prima del test
            gb.getCpu().SP.set(0x0000);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0x31, 0xFE, 0xFF, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.SP.set(0x0000);

        // 12 T-Ticks = 3 M-Cycles totali per completare l'intera operazione bus
        for (int i = 0; i < 12; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA CPU ---
        reporter.incrementAssertions();
        if (cpu.SP.get() != 0xFFFE) {
            reporter.reportFailure(0x31, String.format(
                    "LD SP, nn fallito: Il registro SP contiene 0x%04X, atteso: 0xFFFE", cpu.SP.get()
            ));
        }
    }

    /**
     * Telemetria per osservare il caricamento progressivo a byte dello Stack Pointer
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-30s | SP: 0x%04X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.SP.get(),
                cpu.getTotalTicks()
        );
    }
}