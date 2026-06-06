package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_AddRegister16 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x09, // M1-M2: ADD HL, BC -> 0x0FFF + 0x0001 = 0x1000 (Genera H=1)
                0x19, // M3-M4: ADD HL, DE -> 0x1000 + 0xF000 = 0x0000 (Genera C=1, Z non cambia!)
                0x00  // M5:    NOP
        };

        new Test_AddRegister16().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x0000);

            // Setup dati iniziali
            cpu.HL.set(0x0FFF);
            cpu.BC.set(0x0001);
            cpu.DE.set(0xF000);

            // Forza i flag a 0 per vedere l'accensione di H e C, e la stabilità di Z
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x09, 0x19, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.HL.set(0x0FFF);
        cpu.BC.set(0x0001);
        cpu.DE.set(0xF000);
        cpu.F.set(0x00);

        // --- Passo 1: ADD HL, BC (2 M-Cycles = 8 T-Ticks) ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.HL.get() != 0x1000) {
            reporter.reportFailure(0x09, String.format("ADD HL, BC fallito: HL è 0x%04X, atteso 0x1000", cpu.HL.get()));
        }

        // Flag attesi: Z=0 (inv), N=0, H=1 (riporto bit 11), C=0 -> [--H-] (Raw: 0x20)
        int expectedFlags1 = FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x09, String.format("ADD HL, BC flag falliti: F=0x%02X, atteso 0x%02X [--H-]", cpu.F.get(), expectedFlags1));
        }

        // --- Passo 2: ADD HL, DE (2 M-Cycles = 8 T-Ticks) ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.HL.get() != 0x0000) {
            reporter.reportFailure(0x19, String.format("ADD HL, DE fallito: HL è 0x%04X, atteso 0x0000", cpu.HL.get()));
        }

        // TRAPPOLA: Anche se HL == 0, lo Zero flag NON deve attivarsi!
        // Flag attesi: Z=0 (invariato!), N=0, H=0, C=1 (overflow bit 15) -> [---C] (Raw: 0x10)
        int expectedFlags2 = FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0x19, String.format("ADD HL, DE flag falliti (Verifica trappola Zero): F=0x%02X, atteso 0x%02X [---C]", cpu.F.get(), expectedFlags2));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | HL: 0x%04X | BC: 0x%04X | DE: 0x%04X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.HL.get(),
                cpu.BC.get(),
                cpu.DE.get(),
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