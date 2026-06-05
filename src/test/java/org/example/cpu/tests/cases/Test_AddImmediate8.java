package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_AddImmediate8 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xC6, 0xF0, // M1-M2: ADD A, 0xF0 -> Legge l'immediato e somma (2 M-Cycles)
                0xCE, 0x0F, // M3-M4: ADC A, 0x0F -> Legge l'immediato e somma + Carry (2 M-Cycles)
                0x00        // M5:    NOP
        };

        new Test_AddImmediate8().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();

            // Setup dell'accumulatore A e reset Flag
            cpu.A.set(0x20);
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xC6, 0xF0, 0xCE, 0x0F, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x20);
        cpu.F.set(0x00);

        // --- Esecuzione ADD A, 0xF0 -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica: 0x20 + 0xF0 = 0x110 -> a 8 bit diventa 0x10
        // Flag attesi: Z=0, N=0, H=0, C=1 -> [---C]
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x10) {
            reporter.reportFailure(0xC6, String.format("ADD A, n fallito: A contiene 0x%02X, atteso 0x10", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xC6, String.format("ADD A, n fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags1));
        }

        // --- Esecuzione ADC A, 0x0F -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica: 0x10 (A attuale) + 0x0F (immediato) + 1 (Carry In) = 0x20
        // Flag attesi: Z=0, N=0, H=1, C=0 -> [--H-] (Half-carry attivo perché 0x00 + 0x0F + 1 = 0x10 > 0x0F)
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x20) {
            reporter.reportFailure(0xCE, String.format("ADC A, n fallito: A contiene 0x%02X, atteso 0x20", cpu.A.get()));
        }

        int expectedFlags2 = FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0xCE, String.format("ADC A, n fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags2));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-20s | Z(imm): 0x%02X | A: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.Z.get(), // Registro temporaneo in cui solitamente parcheggi l'immediato al volo
                cpu.A.get(),
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