package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_AddSpImmediate8 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0xE8, 0x02, // M1-M4: ADD SP, 2  -> SP da 0x01FF diventa 0x0201. Genera H=1, C=1. Z=0 fisso!
                0xE8, 0xFE, // M5-M8: ADD SP, -2 -> SP da 0x0201 torna a 0x01FF. Pulisce tutti i flag.
                0x00        // M9:    NOP
        };

        new Test_AddSpImmediate8().runAsPipelineTrace(rom, 9, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x0000);
            cpu.SP.set(0x01FF);
            cpu.F.set(FlagsRegister.MASK_Z); // Partiamo con Z=1 per verificare che si spenga fisso
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xE8, 0x02, 0xE8, 0xFE, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.SP.set(0x01FF);
        cpu.F.set(FlagsRegister.MASK_Z);

        // --- Passo 1: ADD SP, 2 (4 M-Cycles = 16 T-Ticks) ---
        for (int i = 0; i < 16; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.SP.get() != 0x0201) {
            reporter.reportFailure(0xE8, String.format("ADD SP, 2 fallito su SP: SP è 0x%04X, atteso 0x0201", cpu.SP.get()));
        }

        // Flag attesi: Z=0 (forzato), N=0 (forzato), H=1, C=1 -> [--HC] (Raw: 0x30)
        int expectedFlags1 = FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xE8, String.format("ADD SP, 2 flag falliti: F=0x%02X, atteso 0x%02X [--HC]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: ADD SP, -2 (4 M-Cycles = 16 T-Ticks) ---
        for (int i = 0; i < 16; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.SP.get() != 0x01FF) {
            reporter.reportFailure(0xE8, String.format("ADD SP, -2 fallito su SP: SP è 0x%04X, atteso 0x01FF", cpu.SP.get()));
        }

        // Flag attesi: Tutti azzerati -> [----]
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != 0x00) {
            reporter.reportFailure(0xE8, String.format("ADD SP, -2 flag falliti: F=0x%02X, atteso 0x00 [----]", cpu.F.get()));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %-2d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | SP: 0x%04X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.SP.get(),
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