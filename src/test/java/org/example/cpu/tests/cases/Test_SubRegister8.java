package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_SubRegister8 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x90, // M1: SUB B -> Sottrai B da A (1 M-Cycle)
                0x99, // M2: SBC A, C -> Sottrai C + Carry da A (1 M-Cycle)
                0x00  // M3: NOP
        };

        new Test_SubRegister8().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();

            // Setup iniziale scenario:
            cpu.A.set(0x30);
            cpu.B.set(0x40);
            cpu.C.set(0x50);
            cpu.F.set(0x00); // Azzariamo i flag
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x90, 0x99, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x30);
        cpu.B.set(0x40);
        cpu.C.set(0x50);
        cpu.F.set(0x00);

        // --- Passo 1: Esecuzione di SUB B (4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica SUB B: 0x30 - 0x40 = -0x10 -> in complemento a 2 a 8-bit fa 0xF0
        // Flag attesi: N=1 (sempre), Z=0, H=1 (0x00 < 0x00 ma c'è prestito al bit 4), C=1 (0x30 < 0x40) -> [-NHC]
        reporter.incrementAssertions();
        if (cpu.A.get() != 0xF0) {
            reporter.reportFailure(0x90, String.format("SUB B fallito: A contiene 0x%02X, atteso 0xF0", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_N | FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x90, String.format("SUB B fallito nei Flag: F=0x%02X, atteso 0x%02X [-NHC]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: Esecuzione di SBC A, C (Altri 4 T-Ticks / 1 M-Cycle) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica SBC A, C: 0xF0 - 0x50 - Carry(1) = 0x9F
        // Flag attesi: N=1 (sottrazione), Z=0, H=0 (0x00 >= 0x00), C=0 (0xF0 >= 0x51) -> [-N--]
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x9F) {
            reporter.reportFailure(0x99, String.format("SBC A, C fallito: A contiene 0x%02X, atteso 0x9F", cpu.A.get()));
        }

        int expectedFlags2 = FlagsRegister.MASK_N;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0x99, String.format("SBC A, C fallito nei Flag: F=0x%02X, atteso 0x%02X [-N--]", cpu.F.get(), expectedFlags2));
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