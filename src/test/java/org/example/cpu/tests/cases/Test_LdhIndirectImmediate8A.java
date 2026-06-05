package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdhIndirectImmediate8A implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xE0, 0x8C, // M1-M3: LDH (0x8C), A -> Scrive A in RAM[0xFF8C] (Richiede 3 M-Cycles)
                0x00,       // M4:    NOP (Overlap)
                0x00        // M5:    NOP
        };

        new Test_LdhIndirectImmediate8A().runAsPipelineTrace(rom, 5, gb -> {
            // Prepariamo l'accumulatore con il valore da scrivere
            gb.getCpu().A.set(0xA5);
            gb.getCpu().W.set(0x00);
            gb.getCpu().Z.set(0x00);

            // Resettiamo la cella di destinazione in HRAM
            gb.getMmu().writeByte(0xFF8C, 0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xE0, 0x8C, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xFF8C, 0x00);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0xA5);

        // 12 T-Ticks = 3 M-Cycles totali per completare la coreografia dei bus
        for (int i = 0; i < 12; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA RAM ---
        int valInRam = mmu.readByte(0xFF8C);

        reporter.incrementAssertions();
        if (valInRam != 0xA5) {
            reporter.reportFailure(0xE0, String.format(
                    "LDH (n), A fallito: La memoria all'indirizzo 0xFF8C contiene 0x%02X, atteso: 0xA5", valInRam
            ));
        }
    }

    /**
     * Telemetria per osservare la composizione dell'indirizzo e la scrittura in tempo reale
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Monitoraggio live della cella target finale in HRAM (0xFF8C)
        int ramSample = mmu.readByte(0xFF8C);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-28s | WZ(Internal): 0x%02X%02X | A: 0x%02X | RAM[0xFF8C]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.W.get(), cpu.Z.get(),
                cpu.A.get(),
                ramSample,
                cpu.getTotalTicks()
        );
    }
}