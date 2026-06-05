package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdAIndirectBcDe implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x0A, // M1-M2: LD A, (BC) -> Legge dalla RAM all'indirizzo contenuto in BC (Richiede 2 M-Cycles)
                0x00, // M3:    NOP
                0x00  // M4:    NOP
        };

        new Test_LdAIndirectBcDe().runAsPipelineTrace(rom, 4, gb -> {
            // Setup iniziale dell'ambiente tramite l'oggetto GameBoy centralizzato
            gb.getCpu().A.set(0x00);

            // Prepariamo la coppia di registri BC affinché punti alla Work RAM (0xC050)
            gb.getCpu().B.set(0xC0);
            gb.getCpu().C.set(0x50);

            // Prepariamo la cella di memoria RAM simulando un dato presente
            gb.getMmu().writeByte(0xC050, 0xE7);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0x0A, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC050, 0xE7); // Scrittura nella RAM

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x00);
        cpu.B.set(0xC0);
        cpu.C.set(0x50);

        // 8 T-Ticks = 2 M-Cycles per completare l'intera coreografia di LD A, (BC)
        for (int i = 0; i < 8; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA CPU ---
        reporter.incrementAssertions();
        if (cpu.A.get() != 0xE7) {
            reporter.reportFailure(0x0A, String.format(
                    "LD A, (BC) fallito: Il registro A contiene 0x%02X, atteso: 0x%02X", cpu.A.get(), 0xE7
            ));
        }
    }

    /**
     * Telemetria personalizzata per osservare il passaggio dei dati attraverso i bus interni
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Leggiamo dinamicamente l'indirizzo attualmente puntato da BC per la tabella
        int targetAddress = (cpu.B.get() << 8) | cpu.C.get();
        int ramSample = mmu.readByte(targetAddress);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | BC: 0x%04X | RAM[0x%04X]: 0x%02X | Z(WZ): 0x%02X | A: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                targetAddress,
                targetAddress,
                ramSample,
                cpu.Z.get(),
                cpu.A.get(),
                cpu.getTotalTicks()
        );
    }
}