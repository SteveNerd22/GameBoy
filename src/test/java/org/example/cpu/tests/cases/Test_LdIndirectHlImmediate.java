package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdIndirectHlImmediate implements CpuTestCase {

    // Esecuzione in modalità Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x26, 0xC0, // M1-M4: LD H, 0xC0
                0x2E, 0x00, // M3-M4: LD L, 0x00 -> HL = 0xC000
                0x36, 0xAA, // M5-M7: LD (HL), 0xAA (Richiede 3 M-Cycles totali!)
                0x00,       // M8:    NOP (Overlap di stabilizzazione)
                0x00        // M9:    NOP
        };

        new Test_LdIndirectHlImmediate().runAsPipelineTrace(rom, 9, gb -> {
            // Setup iniziale dei registri di puntamento
            gb.getCpu().H.set(0x00);
            gb.getCpu().L.set(0x00);
            gb.getCpu().B.set(0x00);

            // Opzionale: azzeriamo il registro temporaneo interno Z al boot
            // gb.getCpu().Z.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = {
                0x26, 0xC0,
                0x2E, 0x00,
                0x36, 0xAA,
                0x00
        };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);

        for (int i = 0; i < 32; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA RAM ---
        int valInRam = mmu.readByte(0xC000, cpu);

        reporter.incrementAssertions();
        if (valInRam != 0xAA) {
            reporter.reportFailure(0x36, String.format(
                    "LD (HL), n fallito: La memoria all'indirizzo 0xC000 contains 0x%02X, atteso: 0xAA", valInRam
            ));
        }
    }

    /**
     * Personalizzazione del log per mostrare l'evoluzione del registro temporaneo hardware Z
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        int ramSample = mmu.readByte(0xC000, cpu);

        // Andiamo a prendere il valore attuale del registro temporaneo interno Z della CPU
        // Adatta la chiamata in base a come hai battezzato il registro Z interno nel tuo codice (es. cpu.Z.get())
        int zRegisterValue = cpu.Z.get();

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | HL: 0x%02X%02X | Z(WZ): 0x%02X | RAM[0xC000]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.H.get(), cpu.L.get(),
                zRegisterValue, // Colonna dedicata al registro temporaneo hardware Z
                ramSample,
                cpu.getTotalTicks()
        );
    }
}