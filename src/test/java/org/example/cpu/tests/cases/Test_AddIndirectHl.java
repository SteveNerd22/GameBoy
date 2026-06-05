package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_AddIndirectHl implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x86, // M1-M2: ADD A, (HL) -> Legge dalla RAM puntata da HL e somma (2 M-Cycles)
                0x8E, // M3-M4: ADC A, (HL) -> Legge dalla RAM puntata da HL e somma + Carry (2 M-Cycles)
                0x00  // M5:    NOP
        };

        new Test_AddIndirectHl().runAsPipelineTrace(rom, 5, gb -> {
            SM83 cpu = gb.getCpu();
            MMU mmu = gb.getMmu();

            // Configuriuamo il puntatore HL su un indirizzo RAM sicuro (WRAM)
            cpu.HL.set(0xC000);

            // Scriviamo i dati dentro la RAM simulata nelle due celle consecutive
            mmu.writeByte(0xC000, 0x01); // Valore per ADD
            mmu.writeByte(0xC001, 0x05); // Valore per ADC

            // Setup dell'accumulatore A e azzeramento Flag
            cpu.A.set(0xFF);
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x86, 0x8E, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);

        // Setup ambiente
        cpu.HL.set(0xC000);
        mmu.writeByte(0xC000, 0x01);
        mmu.writeByte(0xC001, 0x05);
        cpu.A.set(0xFF);
        cpu.F.set(0x00);

        // --- Esecuzione ADD A, (HL) -> 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica: 0xFF + 0x01 = 0x100 -> a 8 bit diventa 0x00
        // Flag attesi: Z=1, N=0, H=1 (0x0F+0x01=0x10), C=1 (0xFF+0x01=0x100 > 0xFF) -> [Z-HC]
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0x86, String.format("ADD A, (HL) fallito: A contiene 0x%02X, atteso 0x00", cpu.A.get()));
        }

        int expectedFlags1 = FlagsRegister.MASK_Z | FlagsRegister.MASK_H | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags1) {
            reporter.reportFailure(0x86, String.format("ADD A, (HL) fallito nei Flag: F=0x%02X, atteso 0x%02X", cpu.F.get(), expectedFlags1));
        }

        // Spostiamo HL sulla seconda cella di memoria per il prossimo opcode prima di pulser
        cpu.HL.set(0xC001);

        // --- Esecuzione ADC A, (HL) -> Altri 2 M-Cycles = 8 T-Ticks ---
        for (int i = 0; i < 8; i++) cpu.pulse();

        // Verifica: 0x00 (A attuale) + 0x05 (RAM) + 1 (Carry In) = 0x06
        // Flag attesi: Tutti azzerati poichè il risultato non è zero e non c'è overflow -> [----]
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x06) {
            reporter.reportFailure(0x8E, String.format("ADC A, (HL) fallito: A contiene 0x%02X, atteso 0x06", cpu.A.get()));
        }

        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != 0) {
            reporter.reportFailure(0x8E, String.format("ADC A, (HL) fallito nei Flag: F=0x%02X, atteso 0x00", cpu.F.get()));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        // Leggiamo dinamicamente il valore puntato da HL per visualizzarlo nel log
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