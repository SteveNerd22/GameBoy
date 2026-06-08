package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdAIndirectHlIncDec implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x2A, // M1-M2: LD A, (HL+) -> Legge da RAM[HL], poi HL aumenta di 1 (2 M-Cycles)
                0x3A, // M3-M4: LD A, (HL-) -> Legge da RAM[HL], poi HL diminuisce di 1 (2 M-Cycles)
                0x00  // M5:    NOP
        };

        new Test_LdAIndirectHlIncDec().runAsPipelineTrace(rom, 5, gb -> {
            // Setup iniziale: HL punta a 0xC080
            gb.getCpu().H.set(0xC0);
            gb.getCpu().L.set(0x80);
            gb.getCpu().A.set(0x00);

            // Prepariamo due celle di RAM consecutive con valori differenti
            gb.getMmu().writeByte(0xC080, 0x11, gb.getCpu()); // Primo valore da leggere (per HL+)
            gb.getMmu().writeByte(0xC081, 0x99, gb.getCpu()); // Secondo valore da leggere (per HL-)
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico (testiamo il comportamento di 0x2A)
        int[] rom = { 0x2A, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC080, 0x11, cpu);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.H.set(0xC0);
        cpu.L.set(0x80);
        cpu.A.set(0x00);

        // 8 T-Ticks = 2 M-Cycles per completare la prima istruzione
        for (int i = 0; i < 8; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONI ---
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x11) {
            reporter.reportFailure(0x2A, String.format(
                    "LD A, (HL+) fallito: Il registro A contiene 0x%02X, atteso: 0x11", cpu.A.get()
            ));
        }

        reporter.incrementAssertions();
        if (cpu.HL.get() != 0xC081) {
            reporter.reportFailure(0x2A, String.format(
                    "LD A, (HL+) post-incremento fallito: HL contiene 0x%04X, atteso: 0xC081", cpu.HL.get()
            ));
        }
    }

    /**
     * Telemetria personalizzata per osservare lo sfasamento temporale tra la lettura in RAM e l'aggiornamento di HL
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        int currentHl = cpu.HL.get();
        int ramSample = mmu.readByte(currentHl, cpu);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-25s | HL: 0x%04X | RAM[HL]: 0x%02X | Z(WZ): 0x%02X | A: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                currentHl,
                ramSample,
                cpu.Z.get(),
                cpu.A.get(),
                cpu.getTotalTicks()
        );
    }
}