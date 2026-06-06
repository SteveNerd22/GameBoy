package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_ComplementAccumulator implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x3E, 0x55, // M1-M2: LD A, 0x55
                0x2F,       // M3:    CPL (Inverte A -> 0xAA. Setta N=1, H=1. Z e C invariati)
                0x00        // M4:    NOP
        };

        new Test_ComplementAccumulator().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x0000);
            // Setup iniziale dei flag: accendiamo Z e C per testare la persistenza
            cpu.F.set(FlagsRegister.MASK_Z | FlagsRegister.MASK_C);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x3E, 0x55, 0x2F, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.F.set(FlagsRegister.MASK_Z | FlagsRegister.MASK_C); // [Z--C]

        // 1. Eseguiamo LD A, 0x55 (2 M-Cycles = 8 Ticks)
        for (int i = 0; i < 8; i++) cpu.pulse();

        // 2. Eseguiamo CPL (1 M-Cycle = 4 Ticks)
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica Accumulatore: NOT 0x55 = 0xAA
        reporter.incrementAssertions();
        if (cpu.A.get() != 0xAA) {
            reporter.reportFailure(0x2F, String.format("CPL fallito: A è 0x%02X, atteso 0xAA", cpu.A.get()));
        }

        // Verifica Flag: Z=1 (prev), N=1 (fisso), H=1 (fisso), C=1 (prev) -> [ZNHC] (0xF0)
        int expectedFlags = FlagsRegister.MASK_Z | FlagsRegister.MASK_N | FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags) {
            reporter.reportFailure(0x2F, String.format("CPL Flag falliti: F=0x%02X, atteso 0x%02X [ZNHC]", cpu.F.get(), expectedFlags));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | A: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
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