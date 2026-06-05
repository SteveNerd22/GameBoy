package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_CompareImmediate8 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0xFE, 0x30, // M1-M2: CP 0x30 -> Confronto Uguaglianza (Atteso: Z=1, N=1)
                0xFE, 0x40, // M3-M4: CP 0x40 -> Confronto Minore     (Atteso: C=1, N=1)
                0xFE, 0x25, // M5-M6: CP 0x25 -> Confronto Maggiore   (Atteso: H=1, N=1)
                0x00        // M7:    NOP
        };

        new Test_CompareImmediate8().runAsPipelineTrace(rom, 7, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.A.set(0x30);
            cpu.F.set(0x00); // Reset flag
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xFE, 0x30, 0xFE, 0x40, 0xFE, 0x25, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x30);
        cpu.F.set(0x00);

        // --- Passo 1: CP 0x30 (Uguaglianza) -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x30) {
            reporter.reportFailure(0xFE, String.format("CP n ha modificato A! A: 0x%02X, atteso 0x30", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_Z | FlagsRegister.MASK_N;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xFE, String.format("CP n fallito nei Flag (Uguaglianza): F=0x%02X, atteso 0x%02X [ZN--]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: CP 0x40 (Minore) -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x30) {
            reporter.reportFailure(0xFE, String.format("Seconda CP n ha modificato A! A: 0x%02X, atteso 0x30", cpu.A.get()));
        }

        int expectedFlags2 = FlagsRegister.MASK_N | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0xFE, String.format("CP n fallito nei Flag (Minore): F=0x%02X, atteso 0x%02X [-N-C]", cpu.F.get(), expectedFlags2));
        }

        // --- Passo 3: CP 0x25 (Maggiore con Half-Borrow) -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x30) {
            reporter.reportFailure(0xFE, String.format("Terza CP n ha modificato A! A: 0x%02X, atteso 0x30", cpu.A.get()));
        }

        int expectedFlags3 = FlagsRegister.MASK_N | FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags3) {
            reporter.reportFailure(0xFE, String.format("CP n fallito nei Flag (Maggiore/Half-Borrow): F=0x%02X, atteso 0x%02X [-NH-]", cpu.F.get(), expectedFlags3));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %-2d | PC: 0x%04X | IR: 0x%02X | Active Op: %-25s | Z(imm): 0x%02X | A: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.Z.get(),
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