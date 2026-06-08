package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdhAIndirectC implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xF2, // M1-M2: LDH A, (C) -> Legge da RAM[0xFF00 + C] dentro A (Richiede 2 M-Cycles)
                0x00, // M3:    NOP
                0x00  // M4:    NOP
        };

        new Test_LdhAIndirectC().runAsPipelineTrace(rom, 4, gb -> {
            // Setup iniziale dell'accumulatore
            gb.getCpu().A.set(0x00);

            // Impostiamo l'offset nel registro C (punteremo a 0xFF00 + 0x85 = 0xFF85)
            gb.getCpu().C.set(0x85);

            // Prepariamo la cella in HRAM con il valore di test
            gb.getMmu().writeByte(0xFF85, 0x7A, gb.getCpu());
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xF2, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xFF85, 0x7A, cpu);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x00);
        cpu.C.set(0x85);

        // 8 T-Ticks = 2 M-Cycles totali per completare l'operazione
        for (int i = 0; i < 8; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA CPU ---
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x7A) {
            reporter.reportFailure(0xF2, String.format(
                    "LDH A, (C) fallito: Il registro A contiene 0x%02X, atteso: 0x7A", cpu.A.get()
            ));
        }
    }

    /**
     * Telemetria personalizzata focalizzata sulla ricostruzione dell'indirizzo High Page (0xFF00 + C)
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Calcoliamo l'indirizzo dinamico basato su C
        int targetAddress = 0xFF00 + (cpu.C.get() & 0xFF);
        int ramSample = mmu.readByte(targetAddress, cpu);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | C: 0x%02X | WZ(Internal): 0x%02X%02X | RAM[0x%04X]: 0x%02X | A: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.C.get(),
                cpu.W.get(), cpu.Z.get(),
                targetAddress,
                ramSample,
                cpu.A.get(),
                cpu.getTotalTicks()
        );
    }
}