package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdIndirectHlIncDecA implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x22, // M1-M2: LD (HL+), A -> Scrive A in RAM[HL], poi HL aumenta di 1 (2 M-Cycles)
                0x32, // M3-M4: LD (HL-), A -> Scrive A in RAM[HL], poi HL diminuisce di 1 (2 M-Cycles)
                0x00  // M5:    NOP
        };

        new Test_LdIndirectHlIncDecA().runAsPipelineTrace(rom, 5, gb -> {
            // Setup iniziale: HL punta a 0xC090 e l'accumulatore contiene 0xBB
            gb.getCpu().H.set(0xC0);
            gb.getCpu().L.set(0xC090 & 0xFF);
            gb.getCpu().A.set(0xBB);

            // Resettiamo le celle di RAM coinvolte
            gb.getMmu().writeByte(0xC090, 0x00);
            gb.getMmu().writeByte(0xC091, 0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico (testiamo il comportamento di 0x22)
        int[] rom = { 0x22, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC090, 0x00);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.H.set(0xC0);
        cpu.L.set(0x90);
        cpu.A.set(0xBB);

        // 8 T-Ticks = 2 M-Cycles per completare la prima istruzione
        for (int i = 0; i < 8; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONI SU RAM E REGISTRO ---
        int valInRam = mmu.readByte(0xC090);
        reporter.incrementAssertions();
        if (valInRam != 0xBB) {
            reporter.reportFailure(0x22, String.format(
                    "LD (HL+), A fallito: La memoria a 0xC090 contiene 0x%02X, atteso: 0xBB", valInRam
            ));
        }

        reporter.incrementAssertions();
        if (cpu.HL.get() != 0xC091) {
            reporter.reportFailure(0x22, String.format(
                    "LD (HL+), A post-incremento fallito: HL contiene 0x%04X, atteso: 0xC091", cpu.HL.get()
            ));
        }
    }

    /**
     * Telemetria personalizzata per osservare il valore scritto nelle celle RAM puntate da HL
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        int currentHl = cpu.HL.get();

        // Leggiamo staticamente i valori di RAM fissi del test per la griglia di telemetria
        int ram0 = mmu.readByte(0xC090);
        int ram1 = mmu.readByte(0xC091);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-25s | HL: 0x%04X | A: 0x%02X | Z(WZ): 0x%02X | RAM[0xC090]: 0x%02X | RAM[0xC091]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                currentHl,
                cpu.A.get(),
                cpu.Z.get(),
                ram0,
                ram1,
                cpu.getTotalTicks()
        );
    }
}