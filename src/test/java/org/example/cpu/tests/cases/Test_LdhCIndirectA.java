package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdhCIndirectA implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xE2, // M1-M2: LDH (C), A -> Scrive A in RAM[0xFF00 + C] (Richiede 2 M-Cycles)
                0x00, // M3:    NOP
                0x00  // M4:    NOP
        };

        new Test_LdhCIndirectA().runAsPipelineTrace(rom, 4, gb -> {
            // Prepariamo l'accumulatore con il dato da stampare in RAM
            gb.getCpu().A.set(0xDD);

            // Impostiamo l'offset nel registro C (destinazione: 0xFF00 + 0x85 = 0xFF85)
            gb.getCpu().C.set(0x85);

            // Inizializziamo la HRAM a 0x00 per costatare il cambiamento live
            gb.getMmu().writeByte(0xFF85, 0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xE2, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xFF85, 0x00);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0xDD);
        cpu.C.set(0x85);

        // 8 T-Ticks = 2 M-Cycles totali per completare la scrittura hardware
        for (int i = 0; i < 8; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA RAM ---
        int valInRam = mmu.readByte(0xFF85);

        reporter.incrementAssertions();
        if (valInRam != 0xDD) {
            reporter.reportFailure(0xE2, String.format(
                    "LDH (C), A fallito: La memoria all'indirizzo 0xFF85 contiene 0x%02X, atteso: 0xDD", valInRam
            ));
        }
    }

    /**
     * Telemetria personalizzata per osservare il fluire del dato verso la High RAM
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Calcoliamo l'indirizzo dinamico basato su C per monitorare la RAM
        int targetAddress = 0xFF00 + (cpu.C.get() & 0xFF);
        int ramSample = mmu.readByte(targetAddress);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | C: 0x%02X | WZ(Internal): 0x%02X%02X | A: 0x%02X | RAM[0x%04X]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.C.get(),
                cpu.W.get(), cpu.Z.get(),
                cpu.A.get(),
                targetAddress,
                ramSample,
                cpu.getTotalTicks()
        );
    }
}