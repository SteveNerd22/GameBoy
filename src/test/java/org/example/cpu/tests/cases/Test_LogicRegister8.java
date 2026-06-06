package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LogicRegister8 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0xA1, // M1: AND C -> A = 0x5F AND 0xF0 = 0x50 (Atteso: H=1, Z=0)
                0xB0, // M2: OR B  -> A = 0x50 OR 0x0A  = 0x5A (Atteso: tutti 0)
                0xAF, // M3: XOR A -> A = 0x5A XOR 0x5A = 0x00 (Atteso: Z=1)
                0x00  // M4: NOP
        };

        new Test_LogicRegister8().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();

            // Setup dati iniziali
            cpu.A.set(0x5F);
            cpu.C.set(0xF0);
            cpu.B.set(0x0A);
            cpu.F.set(0x00); // Reset flag iniziali
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xA1, 0xB0, 0xAF, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x5F);
        cpu.C.set(0xF0);
        cpu.B.set(0x0A);
        cpu.F.set(0x00);

        // --- Passo 1: AND C (1 M-Cycle = 4 T-Ticks) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x50) {
            reporter.reportFailure(0xA1, String.format("AND C fallito: A è 0x%02X, atteso 0x50", cpu.A.get()));
        }

        // Flag attesi per AND: Z=0, N=0, H=1, C=0 -> [--H-] (Raw: 0x20)
        int expectedFlags1 = FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xA1, String.format("AND C fallito nei Flag: F=0x%02X, atteso 0x%02X [--H-]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: OR B (1 M-Cycle = 4 T-Ticks) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x5A) {
            reporter.reportFailure(0xB0, String.format("OR B fallito: A è 0x%02X, atteso 0x5A", cpu.A.get()));
        }

        // Flag attesi per OR: Z=0, N=0, H=0, C=0 -> [----] (Raw: 0x00)
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != 0x00) {
            reporter.reportFailure(0xB0, String.format("OR B fallito nei Flag: F=0x%02X, atteso 0x00 [----]", cpu.F.get()));
        }

        // --- Passo 3: XOR A (1 M-Cycle = 4 T-Ticks) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0xAF, String.format("XOR A fallito: A è 0x%02X, atteso 0x00", cpu.A.get()));
        }

        // Flag attesi per XOR su se stesso: Z=1, N=0, H=0, C=0 -> [Z---] (Raw: 0x80)
        int expectedFlags3 = FlagsRegister.MASK_Z;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags3) {
            reporter.reportFailure(0xAF, String.format("XOR A fallito nei Flag: F=0x%02X, atteso 0x%02X [Z---]", cpu.F.get(), expectedFlags3));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | A: 0x%02X | B: 0x%02X | C: 0x%02X | Flags: %s | Ticks: %d\n",
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