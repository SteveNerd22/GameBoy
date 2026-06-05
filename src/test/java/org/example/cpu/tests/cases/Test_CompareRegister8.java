package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_CompareRegister8 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0xBF, // M1: CP A -> Compara A con A (Atteso: Z=1, N=1)
                0xB8, // M2: CP B -> Compara A con B (Atteso: C=1, N=1)
                0x00  // M3: NOP
        };

        new Test_CompareRegister8().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();

            // Setup iniziale:
            cpu.A.set(0x30);
            cpu.B.set(0x40);
            cpu.F.set(0x00); // Reset flag
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xBF, 0xB8, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x30);
        cpu.B.set(0x40);
        cpu.F.set(0x00);

        // --- Passo 1: CP A (4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica: A deve essere ancora 0x30! Non deve essere azzerato!
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x30) {
            reporter.reportFailure(0xBF, String.format("CP A ha modificato l'accumulatore! A: 0x%02X, atteso 0x30", cpu.A.get()));
        }

        // Flag attesi: Z=1, N=1, H=0, C=0 -> [ZN--]
        int expectedFlags1 = FlagsRegister.MASK_Z | FlagsRegister.MASK_N;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xBF, String.format("CP A fallito nei Flag: F=0x%02X, atteso 0x%02X [ZN--]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: CP B (Altri 4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica: A è ancora 0x30
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x30) {
            reporter.reportFailure(0xB8, String.format("CP B ha modificato l'accumulatore! A: 0x%02X, atteso 0x30", cpu.A.get()));
        }

        // Verifica simulazione 0x30 - 0x40.
        // Flag attesi: Z=0, N=1, H=0, C=1 -> [-N-C]
        int expectedFlags2 = FlagsRegister.MASK_N | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0xB8, String.format("CP B fallito nei Flag: F=0x%02X, atteso 0x%02X [-N-C]", cpu.F.get(), expectedFlags2));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-20s | A: 0x%02X | B: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.A.get(),
                cpu.B.get(),
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