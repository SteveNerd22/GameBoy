package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_CcfScf implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x37, // M1: SCF    -> Setta C=1, pulisce N=0, H=0. Z resta invariato.
                0x3F, // M2: CCF    -> Inverte C (1->0). H prende il vecchio C (H=1).
                0x3F, // M3: CCF    -> Inverte C (0->1). H prende il vecchio C (H=0).
                0x00  // M4: NOP
        };

        new Test_CcfScf().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();
            // Setup: partiamo con Z=1 per verificare la persistenza, e H=1/C=0
            cpu.F.set(FlagsRegister.MASK_Z | FlagsRegister.MASK_H);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x37, 0x3F, 0x3F, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.F.set(FlagsRegister.MASK_Z | FlagsRegister.MASK_H); // Z=1, H=1, N=0, C=0

        // --- Passo 1: SCF (4 T-Ticks) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Flag attesi: Z=1 (invariato), N=0, H=0, C=1 -> [Z--C] (0x80 | 0x10 = 0x90)
        int expectedFlags1 = FlagsRegister.MASK_Z | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x37, String.format("SCF fallito nei Flag: F=0x%02X, atteso 0x%02X [Z--C]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: Primo CCF (4 T-Ticks) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Flag attesi: Z=1, N=0, H=1 (vecchio C), C=0 (invertito) -> [Z-H-] (0x80 | 0x20 = 0xA0)
        int expectedFlags2 = FlagsRegister.MASK_Z | FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0x3F, String.format("Primo CCF fallito nei Flag: F=0x%02X, atteso 0x%02X [Z-H-]", cpu.F.get(), expectedFlags2));
        }

        // --- Passo 3: Secondo CCF (4 T-Ticks) ---
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Flag attesi: Z=1, N=0, H=0 (vecchio C), C=1 (invertito) -> [Z--C] (0x80 | 0x10 = 0x90)
        int expectedFlags3 = FlagsRegister.MASK_Z | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags3) {
            reporter.reportFailure(0x3F, String.format("Secondo CCF fallito nei Flag: F=0x%02X, atteso 0x%02X [Z--C]", cpu.F.get(), expectedFlags3));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
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