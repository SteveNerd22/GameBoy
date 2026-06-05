package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdAIndirectImmediate16 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xFA, 0x00, 0xC2, // M1-M4: LD A, (0xC200) -> Legge dalla RAM all'indirizzo immediato a 16 bit
                0x00,             // M5:    NOP (Overlap di stabilizzazione)
                0x00              // M6:    NOP
        };

        new Test_LdAIndirectImmediate16().runAsPipelineTrace(rom, 6, gb -> {
            // Setup iniziale dei registri
            gb.getCpu().A.set(0x00);
            gb.getCpu().W.set(0x00);
            gb.getCpu().Z.set(0x00);

            // Prepariamo la cella di memoria RAM di destinazione con il valore di test
            gb.getMmu().writeByte(0xC200, 0x3F);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xFA, 0x00, 0xC2, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC200, 0x3F);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x00);

        // 16 T-Ticks = 4 M-Cycles totali per completare l'intera operazione
        for (int i = 0; i < 16; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA CPU ---
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x3F) {
            reporter.reportFailure(0xFA, String.format(
                    "LD A, (nn) fallito: Il registro A contiene 0x%02X, atteso: 0x3F", cpu.A.get()
            ));
        }
    }

    /**
     * Telemetria personalizzata per osservare la composizione di WZ dall'immediato prima dell'accesso in RAM
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Campionamento live della RAM all'indirizzo target 0xC200
        int ramSample = mmu.readByte(0xC200);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-26s | W: 0x%02X | Z: 0x%02X | RAM[0xC200]: 0x%02X | A: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.W.get(),
                cpu.Z.get(),
                ramSample,
                cpu.A.get(),
                cpu.getTotalTicks()
        );
    }
}