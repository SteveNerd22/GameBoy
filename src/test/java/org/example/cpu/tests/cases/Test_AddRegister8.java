package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_AddRegister8 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x80, // M1: ADD A, B -> Somma B ad A (1 M-Cycle)
                0x89, // M2: ADC A, C -> Somma C + Carry ad A (1 M-Cycle)
                0x00  // M3: NOP
        };

        new Test_AddRegister8().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();

            // Scenario di test:
            // 1. ADD: 0x3A + 0xC6 = 0x00 (Genera Zero, Half-Carry e Carry)
            // 2. ADC: 0x00 + 0x05 + Cy(1) = 0x06
            cpu.A.set(0x3A);
            cpu.B.set(0xC6);
            cpu.C.set(0x05);
            cpu.F.set(0x00); // Reset flag iniziali
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x80, 0x89, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x3A);
        cpu.B.set(0xC6);
        cpu.C.set(0x05);
        cpu.F.set(0x00);

        // --- Passo 1: Esecuzione di ADD A, B (4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica ADD A, B: 0x3A + 0xC6 = 0x100 -> troncato a 8-bit fa 0x00
        // Flag attesi: Z (risultato zero), H (0x0A + 0x06 = 0x10 > 0x0F), C (0x3A + 0xC6 = 0x100 > 0xFF)
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0x80, String.format("ADD A, B fallito: A contiene 0x%02X, atteso 0x00", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_Z | FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x80, String.format("ADD A, B fallito nei Flag: F=0x%02X, atteso 0x%02X [Z-HC]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: Esecuzione di ADC A, C (Altri 4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica ADC A, C: 0x00 + 0x05 + Carry(1) = 0x06
        // Flag attesi: Tutti azzerati (risultato non zero, nessun carry)
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x06) {
            reporter.reportFailure(0x89, String.format("ADC A, C fallito: A contiene 0x%02X, atteso 0x06", cpu.A.get()));
        }

        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != 0) {
            reporter.reportFailure(0x89, String.format("ADC A, C fallito nei Flag: F=0x%02X, atteso 0x00 [----]", cpu.F.get()));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-20s | A: 0x%02X | B: 0x%02X | C: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.A.get(),
                cpu.B.get(),
                cpu.C.get(),
                getFlagsString(cpu.F.get()),
                cpu.getTotalTicks()
        );
    }

    private String getFlagsString(int f) {
        return String.format("[%s%s%s%s]",
                (f & FlagsRegister.MASK_Z) != 0 ? "Z" : "-",
                (f & FlagsRegister.MASK_N) != 0 ? "N" : "-",
                (f & FlagsRegister.MASK_H) != 0 ? "H" : "-",
                (f & FlagsRegister.MASK_C) != 0 ? "C" : "-"
        );
    }
}