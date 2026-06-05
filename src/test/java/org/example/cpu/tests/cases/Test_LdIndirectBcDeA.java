package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdIndirectBcDeA implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x12, // M1-M2: LD (DE), A -> Scrive A nella RAM all'indirizzo in DE (Richiede 2 M-Cycles)
                0x00, // M3:    NOP
                0x00  // M4:    NOP
        };

        new Test_LdIndirectBcDeA().runAsPipelineTrace(rom, 4, gb -> {
            // Carichiamo nell'accumulatore A il valore che vogliamo memorizzare in RAM
            gb.getCpu().A.set(0x55);

            // Prepariamo la coppia di registri DE affinché punti alla Work RAM (0xC100)
            gb.getCpu().D.set(0xC1);
            gb.getCpu().E.set(0x00);

            // Ci assicuriamo che la destinazione sia inizialmente vuota
            gb.getMmu().writeByte(0xC100, 0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0x12, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC100, 0x00);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x55);
        cpu.D.set(0xC1);
        cpu.E.set(0x00);

        // 8 T-Ticks = 2 M-Cycles per completare l'intera operazione
        for (int i = 0; i < 8; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA RAM ---
        int valInRam = mmu.readByte(0xC100);

        reporter.incrementAssertions();
        if (valInRam != 0x55) {
            reporter.reportFailure(0x12, String.format(
                    "LD (DE), A fallito: La memoria all'indirizzo 0xC100 contiene 0x%02X, atteso: 0x55", valInRam
            ));
        }
    }

    /**
     * Telemetria personalizzata per osservare il passaggio del dato dall'accumulatore alla RAM
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Calcoliamo l'indirizzo puntato da DE per il campionamento live della RAM
        int targetAddress = (cpu.D.get() << 8) | cpu.E.get();
        int ramSample = mmu.readByte(targetAddress);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | DE: 0x%04X | A: 0x%02X | Z(WZ): 0x%02X | RAM[0x%04X]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                targetAddress,
                cpu.A.get(),
                cpu.Z.get(),
                targetAddress,
                ramSample,
                cpu.getTotalTicks()
        );
    }
}