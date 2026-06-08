package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LogicIndirectHl implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0xA6, // M1-M2: AND (HL) -> A = 0x5F AND RAM[0xC000](0xF0) = 0x50
                0xB6, // M3-M4: OR (HL)  -> A = 0x50 OR  RAM[0xC001](0x0A) = 0x5A
                0xAE, // M5-M6: XOR (HL) -> A = 0x5A XOR RAM[0xC002](0x5A) = 0x00
                0x00  // M7:    NOP
        };

        new Test_LogicIndirectHl().runAsPipelineTrace(rom, 7, gb -> {
            SM83 cpu = gb.getCpu();
            MMU mmu = gb.getMmu();

            // Puntiamo l'indirizzo WRAM iniziale
            cpu.HL.set(0xC000);

            // Prepariamo l'ambiente in memoria RAM
            mmu.writeByte(0xC000, 0xF0, cpu); // Per AND
            mmu.writeByte(0xC001, 0x0A, cpu); // Per OR
            mmu.writeByte(0xC002, 0x5A, cpu); // Per XOR

            // Setup dell'accumulatore e flag
            cpu.A.set(0x5F);
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0xA6, 0xB6, 0xAE, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.HL.set(0xC000);
        mmu.writeByte(0xC000, 0xF0, cpu);
        mmu.writeByte(0xC001, 0x0A, cpu);
        mmu.writeByte(0xC002, 0x5A, cpu);
        cpu.A.set(0x5F);
        cpu.F.set(0x00);

        // --- Passo 1: AND (HL) -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x50) {
            reporter.reportFailure(0xA6, String.format("AND (HL) fallito: A contiene 0x%02X, atteso 0x50", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_H;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0xA6, String.format("AND (HL) fallito nei Flag: F=0x%02X, atteso 0x%02X [--H-]", cpu.F.get(), expectedFlags1));
        }

        // Spostiamo HL sulla RAM successiva per il test dell'OR
        cpu.HL.set(0xC001);

        // --- Passo 2: OR (HL) -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x5A) {
            reporter.reportFailure(0xB6, String.format("OR (HL) fallito: A contiene 0x%02X, atteso 0x5A", cpu.A.get()));
        }

        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != 0x00) {
            reporter.reportFailure(0xB6, String.format("OR (HL) fallito nei Flag: F=0x%02X, atteso 0x00 [----]", cpu.F.get()));
        }

        // Spostiamo HL sulla RAM successiva per il test dello XOR
        cpu.HL.set(0xC002);

        // --- Passo 3: XOR (HL) -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0xAE, String.format("XOR (HL) fallito: A contiene 0x%02X, atteso 0x00", cpu.A.get()));
        }

        int expectedFlags3 = FlagsRegister.MASK_Z;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags3) {
            reporter.reportFailure(0xAE, String.format("XOR (HL) fallito nei Flag: F=0x%02X, atteso 0x%02X [Z---]", cpu.F.get(), expectedFlags3));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        int currentHlAddr = cpu.HL.get();
        int ramValue = (currentHlAddr >= 0xC000 && currentHlAddr <= 0xC002) ? mmu.readByte(currentHlAddr, cpu) : 0x00;

        System.out.printf(
                "M-Cycle: %-2d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | HL(Addr): 0x%04X | RAM[HL]: 0x%02X | A: 0x%02X | Flags: %s | Ticks: %d\n",
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