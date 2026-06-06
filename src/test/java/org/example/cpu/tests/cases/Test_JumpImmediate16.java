package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_JumpImmediate16 implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = new int[0x200];
        rom[0x0000] = 0xC3;
        rom[0x0001] = 0x50; // Byte basso
        rom[0x0002] = 0x01; // Byte alto
        rom[0x0150] = 0x00; // NOP di atterraggio dopo il salto

        // Cambiato a 5 M-Cycles totali:
        // 4 M-Cycles per il JP nn (0xC3) + 1 M-Cycle per il NOP (0x00) ad atterraggio avvenuto.
        new Test_JumpImmediate16().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x0000);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = new int[0x200];
        rom[0x0000] = 0xC3;
        rom[0x0001] = 0x50;
        rom[0x0002] = 0x01;
        rom[0x0150] = 0x00;
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);

        // 1. Eseguiamo l'istruzione JP nn (Richiede 4 M-Cycles = 16 Tanks/Ticks)
        for (int i = 0; i < 16; i++) cpu.pulse();

        // Verifica 1: Il Program Counter deve essere saltato esattamente a 0x0150
        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x0150) {
            reporter.reportFailure(0xC3, String.format("JP nn fallito: PC è a 0x%04X, atteso 0x0150", cpu.PC.get()));
        }

        // 2. Eseguiamo il NOP di atterraggio a 0x0150 (1 M-Cycle = 4 Ticks)
        for (int i = 0; i < 4; i++) cpu.pulse();

        // Verifica 2: Dopo il NOP, il PC deve essere avanzato a 0x0151
        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x0151) {
            reporter.reportFailure(0x00, String.format("NOP post-salto fallito: PC è a 0x%04X, atteso 0x0151", cpu.PC.get()));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | WZ(temp): 0x%02X%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.W.get(), cpu.Z.get(),
                cpu.getTotalTicks()
        );
    }
}