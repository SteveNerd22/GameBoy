package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_Push implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xC5, // M1-M4: PUSH BC -> Decrementa lo stack e scrive B e C (4 M-Cycles)
                0x00, // M5:    NOP (Overlap)
                0x00  // M6:    NOP
        };

        new Test_Push().runAsPipelineTrace(rom, 6, gb -> {
            // Setup dello stack pointer iniziale (Top della memoria alta)
            gb.getCpu().SP.set(0xFFFE);

            // Valore di test caricato in BC
            gb.getCpu().B.set(0x12);
            gb.getCpu().C.set(0x34);

            // Puliamo preventivamente le zone di memoria dello stack
            gb.getMmu().writeByte(0xFFFD, 0x00);
            gb.getMmu().writeByte(0xFFFC, 0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xC5, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xFFFD, 0x00);
        mmu.writeByte(0xFFFC, 0x00);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.SP.set(0xFFFE);
        cpu.B.set(0x12);
        cpu.C.set(0x34);

        // 16 T-Ticks = 4 M-Cycles totali per completare il PUSH hardware
        for (int i = 0; i < 16; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONI SULLO STATO DELLO STACK ---
        int highByteInRam = mmu.readByte(0xFFFD);
        int lowByteInRam = mmu.readByte(0xFFFC);

        reporter.incrementAssertions();
        if (cpu.SP.get() != 0xFFFC) {
            reporter.reportFailure(0xC5, String.format("PUSH BC fallito: SP atteso 0xFFFC, trovato 0x%04X", cpu.SP.get()));
        }

        reporter.incrementAssertions();
        if (highByteInRam != 0x12 || lowByteInRam != 0x34) {
            reporter.reportFailure(0xC5, String.format(
                    "PUSH BC fallito in RAM: RAM[0xFFFD]=0x%02X (Atteso 0x12), RAM[0xFFFC]=0x%02X (Atteso 0x34)",
                    highByteInRam, lowByteInRam
            ));
        }
    }

    /**
     * Telemetria avanzata per visualizzare la discesa dello Stack Pointer e il riempimento della RAM
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        int ramFd = mmu.readByte(0xFFFD);
        int ramFc = mmu.readByte(0xFFFC);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-15s | SP: 0x%04X | BC: 0x%02X%02X | RAM[0xFFFD]: 0x%02X | RAM[0xFFFC]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.SP.get(),
                cpu.B.get(), cpu.C.get(),
                ramFd,
                ramFc,
                cpu.getTotalTicks()
        );
    }
}