package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdIndirectImmediate16A implements CpuTestCase {
    public static void main(String[] args) {
        int[] rom = {
                0xEA, 0x00, 0xC3, // M1-M4: LD (0xC300), A -> Scrive A all'indirizzo immediato
                0x00,             // M5:    NOP (Overlap)
                0x00              // M6:    NOP
        };

        new Test_LdIndirectImmediate16A().runAsPipelineTrace(rom, 6, gb -> {
            // Setup iniziale dell'accumulatore con il valore da scrivere
            gb.getCpu().A.set(0xBE);
            gb.getCpu().W.set(0x00);
            gb.getCpu().Z.set(0x00);

            // Inizializziamo la RAM di destinazione a 0x00 per verificare il cambiamento
            gb.getMmu().writeByte(0xC300, 0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xEA, 0x00, 0xC3, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC300, 0x00);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0xBE);

        // 16 T-Ticks = 4 M-Cycles totali per completare la scrittura
        for (int i = 0; i < 16; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA RAM ---
        int valInRam = mmu.readByte(0xC300);

        reporter.incrementAssertions();
        if (valInRam != 0xBE) {
            reporter.reportFailure(0xEA, String.format(
                    "LD (nn), A fallito: La memoria all'indirizzo 0xC300 contiene 0x%02X, atteso: 0xBE", valInRam
            ));
        }
    }

    /**
     * Telemetria personalizzata per osservare il passaggio del valore di A verso la RAM
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Monitoraggio live della cella RAM di destinazione
        int ramSample = mmu.readByte(0xC300);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-26s | W: 0x%02X | Z: 0x%02X | A: 0x%02X | RAM[0xC300]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.W.get(),
                cpu.Z.get(),
                cpu.A.get(),
                ramSample,
                cpu.getTotalTicks()
        );
    }
}