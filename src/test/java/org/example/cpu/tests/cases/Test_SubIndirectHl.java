package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_SubIndirectHl implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x96, // M1-M2: SUB (HL)   -> Sottrai RAM[HL] da A (2 M-Cycles)
                0x9E, // M3-M4: SBC A, (HL) -> Sottrai RAM[HL] + Carry da A (2 M-Cycles)
                0x00  // M5:    NOP
        };

        new Test_SubIndirectHl().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();
            MMU mmu = gb.getMmu();

            // Configuriuamo HL sulla RAM di lavoro (WRAM)
            cpu.HL.set(0xC000);

            // Scriviamo i valori di test nelle celle di memoria puntate
            mmu.writeByte(0xC000, 0x01); // Valore per SUB (0x00 - 0x01 = 0xFF)
            mmu.writeByte(0xC001, 0xFE); // Valore per SBC (0xFF - 0xFE - 1 = 0x00)

            // Setup iniziale dei registri
            cpu.A.set(0x00);
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x96, 0x9E, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);

        cpu.HL.set(0xC000);
        mmu.writeByte(0xC000, 0x01);
        mmu.writeByte(0xC001, 0xFE);
        cpu.A.set(0x00);
        cpu.F.set(0x00);

        // --- Passo 1: SUB (HL) -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica SUB: 0x00 - 0x01 = 0xFF
        // Flag attesi: Z=0, N=1, H=1, C=1 -> [-NHC]
        reporter.incrementAssertions();
        if (cpu.A.get() != 0xFF) {
            reporter.reportFailure(0x96, String.format("SUB (HL) fallito: A contiene 0x%02X, atteso 0xFF", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_N | FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x96, String.format("SUB (HL) fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags1));
        }

        // Spostiamo HL sulla cella successiva per il secondo test prima di fare i pulse
        cpu.HL.set(0xC001);

        // --- Passo 2: SBC A, (HL) -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica SBC: 0xFF - 0xFE - Carry(1) = 0x00
        // Flag attesi: Z=1, N=1, H=0, C=0 -> [ZN--] (Risultato zero, nessun prestito ulteriore)
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0x9E, String.format("SBC A, (HL) fallito: A contiene 0x%02X, atteso 0x00", cpu.A.get()));
        }

        int expectedFlags2 = FlagsRegister.MASK_Z | FlagsRegister.MASK_N;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0x9E, String.format("SBC A, (HL) fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags2));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        int currentHlAddr = cpu.HL.get();
        int ramValue = mmu.readByte(currentHlAddr);

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