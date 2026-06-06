package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_IncDecRegister16 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x03, // M1-M2: INC BC -> BC da 0x00FF diventa 0x0100. Flags invariati.
                0x2B, // M3-M4: DEC HL -> HL da 0x0000 diventa 0xFFFF. Flags invariati.
                0x00  // M5:    NOP
        };

        new Test_IncDecRegister16().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x0000);

            // Setup registri a 16 bit
            cpu.BC.set(0x00FF);
            cpu.HL.set(0x0000);

            // Setup flag arbitrari per testare la persistenza
            cpu.F.set(FlagsRegister.MASK_Z | FlagsRegister.MASK_H); // [Z-H-]
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x03, 0x2B, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.BC.set(0x00FF);
        cpu.HL.set(0x0000);

        int originalFlags = FlagsRegister.MASK_Z | FlagsRegister.MASK_H;
        cpu.F.set(originalFlags);

        // --- Passo 1: INC BC (2 M-Cycles = 8 T-Ticks) ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.BC.get() != 0x0100) {
            reporter.reportFailure(0x03, String.format("INC BC fallito: BC è 0x%04X, atteso 0x0100", cpu.BC.get()));
        }

        // I flag non devono essere stati toccati
        reporter.incrementAssertions();
        if (cpu.F.get() != originalFlags) {
            reporter.reportFailure(0x03, String.format("INC BC ha modificato i Flag abusivamente: F=0x%02X, atteso 0x%02X", cpu.F.get(), originalFlags));
        }

        // --- Passo 2: DEC HL (2 M-Cycles = 8 T-Ticks) ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.HL.get() != 0xFFFF) {
            reporter.reportFailure(0x2B, String.format("DEC HL fallito: HL è 0x%04X, atteso 0xFFFF", cpu.HL.get()));
        }

        // I flag devono essere ancora intatti
        reporter.incrementAssertions();
        if (cpu.F.get() != originalFlags) {
            reporter.reportFailure(0x2B, String.format("DEC HL ha modificato i Flag abusivamente: F=0x%02X, atteso 0x%02X", cpu.F.get(), originalFlags));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | BC: 0x%04X | HL: 0x%04X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.BC.get(),
                cpu.HL.get(),
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