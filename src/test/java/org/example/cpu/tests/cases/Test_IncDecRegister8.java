package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_IncDecRegister8 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x04, // M1: INC B -> 0x0F -> 0x10 (Atteso: H=1, N=0, C invariato)
                0x0D, // M2: DEC C -> 0x00 -> 0xFF (Atteso: H=1, N=1, C invariato)
                0x00  // M3: NOP
        };

        new Test_IncDecRegister8().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();

            // Setup iniziale:
            cpu.B.set(0x0F);
            cpu.C.set(0x00);
            cpu.F.set(0x10); // Accendiamo il Carry (C=1) artificialmente per vedere se viene preservato!
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x04, 0x0D, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.B.set(0x0F);
        cpu.C.set(0x00);
        cpu.F.set(0x10); // C=1 iniziale

        // --- Passo 1: INC B (4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.B.get() != 0x10) {
            reporter.reportFailure(0x04, String.format("INC B fallito: B contiene 0x%02X, atteso 0x10", cpu.B.get()));
        }

        // Flag attesi: Z=0, N=0, H=1, C=1 (preservato) -> [--HC] -> 0x20 | 0x10 = 0x30
        int expectedFlags1 = FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x04, String.format("INC B fallito nei Flag: F=0x%02X, atteso 0x%02X [--HC]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: DEC C (Altri 4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.C.get() != 0xFF) {
            reporter.reportFailure(0x0D, String.format("DEC C fallito: C contiene 0x%02X, atteso 0xFF", cpu.C.get()));
        }

        // Flag attesi: Z=0, N=1, H=1, C=1 (preservato) -> [-NHC] -> 0x40 | 0x20 | 0x10 = 0x70
        int expectedFlags2 = FlagsRegister.MASK_N | FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0x0D, String.format("DEC C fallito nei Flag: F=0x%02X, atteso 0x%02X [-NHC]", cpu.F.get(), expectedFlags2));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | B: 0x%02X | C: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
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