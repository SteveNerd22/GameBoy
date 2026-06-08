package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdIndirectImmediate16Sp implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x08, 0x00, 0xC5, // M1-M5: LD (0xC500), SP -> Scrive lo Stack Pointer in due celle RAM
                0x00,             // M6:    NOP (Overlap)
                0x00              // M7:    NOP
        };

        new Test_LdIndirectImmediate16Sp().runAsPipelineTrace(rom, 7, gb -> {
            // Setup dello Stack Pointer con il valore di test
            gb.getCpu().SP.set(0xFFFE);
            gb.getCpu().W.set(0x00);
            gb.getCpu().Z.set(0x00);

            // Puliamo la RAM di destinazione per rilevare la scrittura hardware
            gb.getMmu().writeByte(0xC500, 0x00, gb.getCpu());
            gb.getMmu().writeByte(0xC501, 0x00, gb.getCpu());
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0x08, 0x00, 0xC5, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC500, 0x00, cpu);
        mmu.writeByte(0xC501, 0x00, cpu);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.SP.set(0xFFFE);

        // 20 T-Ticks = 5 M-Cycles totali per completare l'intera operazione
        for (int i = 0; i < 20; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONI SULLA RAM ---
        int ramLow = mmu.readByte(0xC500, cpu);
        int ramHigh = mmu.readByte(0xC501, cpu);

        reporter.incrementAssertions();
        if (ramLow != 0xFE || ramHigh != 0xFF) {
            reporter.reportFailure(0x08, String.format(
                    "LD (nn), SP fallito: RAM[0xC500]=0x%02X, RAM[0xC501]=0x%02X. Atteso: 0xFE, 0xFF",
                    ramLow, ramHigh
            ));
        }
    }

    /**
     * Telemetria dedicata per seguire il doppio ciclo di scrittura sequenziale in RAM
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Monitoraggio in tempo reale delle due celle di Work RAM
        int ramLow = mmu.readByte(0xC500, cpu);
        int ramHigh = mmu.readByte(0xC501, cpu);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-26s | SP: 0x%04X | WZ: 0x%02X%02X | RAM[0xC500]: 0x%02X | RAM[0xC501]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.SP.get(),
                cpu.W.get(), cpu.Z.get(),
                ramLow,
                ramHigh,
                cpu.getTotalTicks()
        );
    }
}