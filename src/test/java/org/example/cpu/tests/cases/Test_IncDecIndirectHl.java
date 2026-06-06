package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_IncDecIndirectHl implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x34, // M1-M3: INC (HL) -> RAM[0xC000] (0x0F -> 0x10)
                0x35, // M4-M6: DEC (HL) -> RAM[0xC001] (0x01 -> 0x00)
                0x00  // M7: NOP
        };

        new Test_IncDecIndirectHl().runAsPipelineTrace(rom, 7, gb -> {
            SM83 cpu = gb.getCpu();
            MMU mmu = gb.getMmu();

            // Configurazione puntatore HL sulla WRAM
            cpu.HL.set(0xC000);

            // Scriviamo i valori iniziali nelle celle di memoria
            mmu.writeByte(0xC000, 0x0F); // Per il test dell'INC (Half-Carry)
            mmu.writeByte(0xC001, 0x01); // Per il test del DEC (Zero Flag)

            // Accendiamo il Carry artificialmente per essere certi che non venga toccato
            cpu.F.set(FlagsRegister.MASK_C);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x34, 0x35, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.HL.set(0xC000);
        mmu.writeByte(0xC000, 0x0F);
        mmu.writeByte(0xC001, 0x01);
        cpu.F.set(FlagsRegister.MASK_C); // C=1 iniziale

        // --- Passo 1: INC (HL) -> 3 M-Cycles = 12 T-Ticks ---
        for (int i = 0; i < 12; i++) cpu.pulse();

        int val1 = mmu.readByte(0xC000);
        reporter.incrementAssertions();
        if (val1 != 0x10) {
            reporter.reportFailure(0x34, String.format("INC (HL) fallito in RAM: RAM[0xC000] è 0x%02X, atteso 0x10", val1));
        }

        // Flag attesi: Z=0, N=0, H=1, C=1 (preservato) -> [--HC]
        int expectedFlags1 = FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x34, String.format("INC (HL) fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags1));
        }

        // Spostiamo HL sulla cella successiva per il secondo test prima dei pulse
        cpu.HL.set(0xC001);

        // --- Passo 2: DEC (HL) -> Altri 3 M-Cycles = 12 T-Ticks ---
        for (int i = 0; i < 12; i++) cpu.pulse();

        int val2 = mmu.readByte(0xC001);
        reporter.incrementAssertions();
        if (val2 != 0x00) {
            reporter.reportFailure(0x35, String.format("DEC (HL) fallito in RAM: RAM[0xC001] è 0x%02X, atteso 0x00", val2));
        }

        // Flag attesi: Z=1, N=1, H=0, C=1 (preservato) -> [ZN-C]
        int expectedFlags2 = FlagsRegister.MASK_Z | FlagsRegister.MASK_N | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0x35, String.format("DEC (HL) fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags2));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        int currentHl = cpu.HL.get();
        int ramVal = (currentHl >= 0xC000 && currentHl <= 0xC001) ? mmu.readByte(currentHl) : 0x00;

        System.out.printf(
                "M-Cycle: %-2d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | HL: 0x%04X | RAM[HL]: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                currentHl,
                ramVal,
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