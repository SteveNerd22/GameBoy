package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LogicImmediate8 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0xE6, 0xF0, // M1-M2: AND 0xF0 -> A = 0x5F AND 0xF0 = 0x50
                0xF6, 0x0A, // M3-M4: OR 0x0A  -> A = 0x50 OR  0x0A  = 0x5A
                0xEE, 0x5A, // M5-M6: XOR 0x5A -> A = 0x5A XOR 0x5A = 0x00
                0x00        // M7:    NOP
        };

        new Test_LogicImmediate8().runAsPipelineTrace(rom, 7, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.A.set(0x5F);
            cpu.F.set(0x00); // Reset flag iniziali
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xE6, 0xF0, 0xF6, 0x0A, 0xEE, 0x5A, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x5F);
        cpu.F.set(0x00);

        // --- Passo 1: AND 0xF0 -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x50) {
            reporter.reportFailure(0xE6, String.format("AND n fallito: A è 0x%02X, atteso 0x50", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xE6, String.format("AND n fallito nei Flag: F=0x%02X, atteso 0x%02X [--H-]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: OR 0x0A -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x5A) {
            reporter.reportFailure(0xF6, String.format("OR n fallito: A è 0x%02X, atteso 0x5A", cpu.A.get()));
        }

        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != 0x00) {
            reporter.reportFailure(0xF6, String.format("OR n fallito nei Flag: F=0x%02X, atteso 0x00 [----]", cpu.F.get()));
        }

        // --- Passo 3: XOR 0x5A -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0xEE, String.format("XOR n fallito: A è 0x%02X, atteso 0x00", cpu.A.get()));
        }

        int expectedFlags3 = FlagsRegister.MASK_Z;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags3) {
            reporter.reportFailure(0xEE, String.format("XOR n fallito nei Flag: F=0x%02X, atteso 0x%02X [Z---]", cpu.F.get(), expectedFlags3));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %-2d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | Z(imm): 0x%02X | A: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.Z.get(), // Registro temporaneo dove parcheggi l'immediato letto
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