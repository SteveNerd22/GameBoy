package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LoadIndirectHlReg implements CpuTestCase {

    // Esecuzione in modalità Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0x26, 0xC0, // LD H, 0xC0
                0x2E, 0x00, // LD L, 0x00 -> HL = 0xC000
                0x70,       // LD (HL), B  (Richiede 2 M-Cycles - Scrive il valore di B in RAM)
                0x00,       // NOP
                0x00        // NOP
        };

        new Test_LoadIndirectHlReg().runAsPipelineTrace(rom, 8, gb -> {
            // Setup iniziale dei registri tramite l'oggetto GameBoy centralizzato
            gb.getCpu().H.set(0x00);
            gb.getCpu().L.set(0x00);

            // Iniettiamo nel registro sorgente B il valore che vogliamo veder comparire in RAM
            gb.getCpu().B.set(0xAA);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = {
                0x26, 0xC0,
                0x2E, 0x00,
                0x70,
                0x00
        };
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.B.set(0xAA); // Setup del registro sorgente

        // 28 T-Ticks = 7 M-Cycles totali per completare la scrittura e l'overlap del NOP
        for (int i = 0; i < 28; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONE SULLA RAM ---
        // Verifichiamo se l'hardware ha effettivamente scritto il valore di B all'indirizzo contenuto in HL
        int valInRam = mmu.readByte(0xC000);

        reporter.incrementAssertions();
        if (valInRam != 0xAA) {
            reporter.reportFailure(0x70, String.format(
                    "LD (HL), B fallito: La memoria all'indirizzo 0xC000 contiene 0x%02X, atteso 0x%AA", valInRam
            ));
        }
    }

    /**
     * Personalizziamo la telemetria includendo anche un'ispezione live della cella di memoria RAM 0xC000
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        // Leggiamo al volo l'indirizzo 0xC000 per stamparlo in tempo reale nella tabella
        int ramSample = mmu.readByte(0xC000);
        // Nota: se cpu non ha getMmu(), nel main del test puoi recuperarlo iniettandolo temporaneamente,
        // oppure stampare solo i registri. Se hai dubbi lascia 0x00 o passa l'istanza.

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-19s | HL: 0x%02X%02X | B: 0x%02X | RAM[0xC000]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.H.get(), cpu.L.get(),
                cpu.B.get(),
                ramSample,
                cpu.getTotalTicks()
        );
    }
}