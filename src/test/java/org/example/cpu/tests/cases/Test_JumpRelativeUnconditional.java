package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_JumpRelativeUnconditional implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = new int[0x200];

        // Caso 1: Salto in avanti (+4)
        rom[0x0000] = 0x18;
        rom[0x0001] = 0x04; // Offset positivo
        rom[0x0006] = 0x00; // NOP di atterraggio (0x0002 + 4 = 0x0006)

        // Caso 2: Salto all'indietro (-18 -> 0xEE)
        rom[0x0010] = 0x18;
        rom[0x0011] = 0xEE; // Offset negativo (-18). Destinazione attesa: 0x0012 - 18 = 0x0000

        // Eseguiamo la traccia sul primo caso
        new Test_JumpRelativeUnconditional().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x0000);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = new int[0x200];
        rom[0x0000] = 0x18; rom[0x0001] = 0x04; rom[0x0006] = 0x00;
        rom[0x0010] = 0x18; rom[0x0011] = 0xEE;
        mmu.loadCartridge(rom);

        // --- TEST 1: Salto in Avanti (Offset Positivo) ---
        cpu.reset();
        cpu.PC.set(0x0000);

        // 1 M-Cycle Fetch + 3 Step di esecuzione (case 0, 1, 2) = 4 M-Cycles (16 Ticks)
        for (int i = 0; i < 16; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x0006) {
            reporter.reportFailure(0x18, String.format("JR e (Avanti) fallito: PC è a 0x%04X, atteso 0x0006", cpu.PC.get()));
        }

        // --- TEST 2: Salto all'Indietro (Offset Negativo con propagazione IDU) ---
        cpu.reset();
        cpu.PC.set(0x0010); // Partiamo da 0x0010

        // Eseguiamo i 4 M-Cycles completi per il salto all'indietro
        for (int i = 0; i < 16; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x0000) {
            reporter.reportFailure(0x18, String.format("JR e (Indietro) fallito: PC è a 0x%04X, atteso 0x0000", cpu.PC.get()));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-25s | WZ: 0x%04X | Z: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.WZ.get(),
                cpu.Z.get(),
                cpu.getTotalTicks()
        );
    }
}