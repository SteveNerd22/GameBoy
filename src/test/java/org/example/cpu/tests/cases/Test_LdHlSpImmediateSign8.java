package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdHlSpImmediateSign8 implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xF8, 0xFA, // M1-M3: LD HL, SP+(-6) -> Calcola l'offset e aggiorna HL e Flag (3 M-Cycles)
                0x00,       // M4:    NOP (Overlap)
                0x00        // M5:    NOP
        };

        new Test_LdHlSpImmediateSign8().runAsPipelineTrace(rom, 5, gb -> {
            // Setup iniziale: SP posizionato a 0xFFFE
            gb.getCpu().SP.set(0xFFFE);

            // Azzariamo HL e Flags per vedere le modifiche reali
            gb.getCpu().H.set(0x00);
            gb.getCpu().L.set(0x00);
            gb.getCpu().F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xF8, 0xFA, 0x00 };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.SP.set(0xFFFE);
        cpu.H.set(0x00);
        cpu.L.set(0x00);
        cpu.F.set(0x00);

        // 12 T-Ticks = 3 M-Cycles totali
        for (int i = 0; i < 12; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONI SULLO STATO DELLA CPU ---
        // Calcolo atteso: 0xFFFE + (-6) = 0xFFF8
        reporter.incrementAssertions();
        if (cpu.HL.get() != 0xFFF8) {
            reporter.reportFailure(0xF8, String.format(
                    "LD HL, SP+e fallito nel calcolo: HL contiene 0x%04X, atteso: 0xFFF8", cpu.HL.get()
            ));
        }

        // Verifica dei Flag hardware per SP=0xFFFE e offset=0xFA:
        // (0x0E + 0x0A) = 0x18 (> 0x0F) -> Half Carry attivo (H)
        // (0x0FE + 0xFA) = 0x1F8 (> 0xFF) -> Carry attivo (C)
        // Ci aspettiamo quindi H e C a 1, Z e N a 0.
        int expectedF = FlagsRegister.MASK_H | FlagsRegister.MASK_C;

        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedF) {
            reporter.reportFailure(0xF8, String.format(
                    "LD HL, SP+e fallito nei Flag: F contiene 0x%02X, attesi solo H e C (0x%02X)",
                    cpu.F.get(), expectedF
            ));
        }
    }

    /**
     * Telemetria per osservare la conversione del segno del parametro e l'impatto sui Flag
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-26s | SP: 0x%04X | Z(offset): 0x%02X | HL: 0x%04X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.SP.get(),
                cpu.Z.get(),
                cpu.HL.get(),
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