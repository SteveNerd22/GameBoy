package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_CompareIndirectHl implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0xBE,            // M1-M2: CP (HL) -> Compara A con RAM[0xC000] (Atteso: Z=1, N=1)
                0xBE,            // M6-M7: CP (HL) -> Compara A con RAM[0xC001] (Atteso: C=1, N=1)
                0x00             // M8: NOP
        };

        new Test_CompareIndirectHl().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();
            MMU mmu = gb.getMmu();

            // Puntiamo l'indirizzo WRAM 0xC000
            cpu.HL.set(0xC000);

            // Carichiamo i valori nella RAM simulata
            mmu.writeByte(0xC000, 0x45, cpu); // Identico ad A
            mmu.writeByte(0xC001, 0x50, cpu); // Maggiore di A (genera Carry)

            // Setup dell'accumulatore
            cpu.A.set(0x45);
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xBE, 0xBE, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.HL.set(0xC000);
        mmu.writeByte(0xC000, 0x45, cpu);
        mmu.writeByte(0xC001, 0x50, cpu);
        cpu.A.set(0x45);
        cpu.F.set(0x00);

        // --- Passo 1: Prima CP (HL) [Confronto Uguaglianza] -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // L'accumulatore DEVE essere rimasto intatto
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x45) {
            reporter.reportFailure(0xBE, String.format("CP (HL) ha modificato A! A: 0x%02X, atteso 0x45", cpu.A.get()));
        }

        // Flag attesi: Z=1, N=1, H=0, C=0 -> [ZN--]
        int expectedFlags1 = FlagsRegister.MASK_Z | FlagsRegister.MASK_N;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xBE, String.format("CP (HL) fallito nei Flag (Uguaglianza): F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags1));
        }

        // Spostiamo il puntatore sulla seconda cella di memoria
        cpu.HL.set(0xC001);

        // --- Passo 2: Seconda CP (HL) [Confronto Minore] -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // A deve essere ancora 0x45
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x45) {
            reporter.reportFailure(0xBE, String.format("Seconda CP (HL) ha modificato A! A: 0x%02X, atteso 0x45", cpu.A.get()));
        }

        // Flag attesi: Z=0, N=1, H=1 (0x05 < 0x00), C=1 (0x45 < 0x50) -> [-NHC]
        int expectedFlags2 = FlagsRegister.MASK_N | FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0xBE, String.format("CP (HL) fallito nei Flag (Minore): F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags2));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        int currentHlAddr = cpu.HL.get();
        int ramValue = mmu.readByte(currentHlAddr, cpu);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-20s | HL(Addr): 0x%04X | RAM[HL]: 0x%02X | A: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                currentHlAddr,
                ramValue,
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