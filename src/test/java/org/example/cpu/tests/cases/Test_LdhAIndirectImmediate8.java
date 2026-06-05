package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdhAIndirectImmediate8 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xF0, 0x88, // M1-M3: LDH A, (0x88) -> Legge da RAM[0xFF88] dentro A (Richiede 3 M-Cycles)
                0x00,       // M4:    NOP (Overlap)
                0x00        // M5:    NOP
        };

        new Test_LdhAIndirectImmediate8().runAsPipelineTrace(rom, 5, gb -> {
            // Inizializziamo l'accumulatore e i registri ombra
            gb.getCpu().A.set(0x00);
            gb.getCpu().W.set(0x00);
            gb.getCpu().Z.set(0x00);

            // Prepariamo la cella in HRAM con il valore che l'istruzione dovrà leggere
            gb.getMmu().writeByte(0xFF88, 0x4B);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xF0, 0x88, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xFF88, 0x4B);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x00);

        // 12 T-Ticks = 3 M-Cycles totali per completare l'intera operazione bus
        for (int i = 0; i < 12; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA CPU ---
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x4B) {
            reporter.reportFailure(0xF0, String.format(
                    "LDH A, (n) fallito: Il registro A contiene 0x%02X, atteso: 0x4B", cpu.A.get()
            ));
        }
    }

    /**
     * Telemetria per osservare la cattura di 'n' e la successiva lettura all'indirizzo generato 0xFFn
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Monitoraggio della cella target finale in HRAM (0xFF88)
        int ramSample = mmu.readByte(0xFF88);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-28s | WZ(Internal): 0x%02X%02X | RAM[0xFF88]: 0x%02X | A: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.W.get(), cpu.Z.get(),
                ramSample,
                cpu.A.get(),
                cpu.getTotalTicks()
        );
    }
}