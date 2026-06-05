package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_SubImmediate8 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xD6, 0x40, // M1-M2: SUB A, 0x40 -> Legge l'immediato e sottrae (2 M-Cycles)
                0xDE, 0x50, // M3-M4: SBC A, 0x50 -> Legge l'immediato e sottrae + Carry (2 M-Cycles)
                0x00        // M5:    NOP
        };

        new Test_SubImmediate8().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();

            // Setup iniziale dell'accumulatore e azzeramento Flag
            cpu.A.set(0x30);
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xD6, 0x40, 0xDE, 0x50, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x30);
        cpu.F.set(0x00);

        // --- Esecuzione SUB A, 0x40 -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica: 0x30 - 0x40 = 0xF0
        // Flag attesi: Z=0, N=1, H=0, C=1 -> [-N-C]
        reporter.incrementAssertions();
        if (cpu.A.get() != 0xF0) {
            reporter.reportFailure(0xD6, String.format("SUB A, n fallito: A contiene 0x%02X, atteso 0xF0", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_N | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xD6, String.format("SUB A, n fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags1));
        }

        // --- Esecuzione SBC A, 0x50 -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica: 0xF0 - 0x50 - Carry(1) = 0x9F
        // Flag attesi: Z=0, N=1, H=1, C=0 -> [-NH-] (Half-Borrow attivo perché 0x0 - 0x0 - 1 richiede prestito)
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x9F) {
            reporter.reportFailure(0xDE, String.format("SBC A, n fallito: A contiene 0x%02X, atteso 0x9F", cpu.A.get()));
        }

        int expectedFlags2 = FlagsRegister.MASK_N | FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0xDE, String.format("SBC A, n fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags2));
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
                cpu.Z.get(), // Registro di transito dove memorizzi l'immediato inline
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