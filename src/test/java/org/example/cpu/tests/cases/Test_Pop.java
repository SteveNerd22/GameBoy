package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_Pop implements CpuTestCase {

    // Esecuzione Standalone con tracciamento della Pipeline
    public static void main(String[] args) {
        int[] rom = {
                0xC1, // M1-M3: POP BC -> Estrae dallo stack e incrementa SP (3 M-Cycles)
                0x00, // M4:    NOP (Overlap)
                0x00  // M5:    NOP
        };

        new Test_Pop().runAsPipelineTrace(rom, 5, gb -> {
            // Posizioniamo lo stack pointer sul fondo dei dati inseriti
            gb.getCpu().SP.set(0xFFFC);

            // Azzariamo BC per assicurarci che cambi valore durante il test
            gb.getCpu().B.set(0x00);
            gb.getCpu().C.set(0x00);

            // Carichiamo i dati fittizi in RAM che simulano un precedente PUSH (Little Endian nello stack)
            gb.getMmu().writeByte(0xFFFC, 0x78); // LSB (Finirà in C)
            gb.getMmu().writeByte(0xFFFD, 0x56); // MSB (Finirà in B)
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica fissa per il TestSuiteRunner automatico
        int[] rom = { 0xC1, 0x00 };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xFFFC, 0x78);
        mmu.writeByte(0xFFFD, 0x56);

        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.SP.set(0xFFFC);
        cpu.B.set(0x00);
        cpu.C.set(0x00);

        // 12 T-Ticks = 3 M-Cycles totali per completare l'intera operazione bus
        for (int i = 0; i < 12; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONI SULLO STATO DELLA CPU ---
        reporter.incrementAssertions();
        if (cpu.SP.get() != 0xFFFE) {
            reporter.reportFailure(0xC1, String.format("POP BC fallito: SP atteso 0xFFFE, trovato 0x%04X", cpu.SP.get()));
        }

        reporter.incrementAssertions();
        if (cpu.B.get() != 0x56 || cpu.C.get() != 0x78) {
            reporter.reportFailure(0xC1, String.format(
                    "POP BC fallito nei registri: B=0x%02X (Atteso 0x56), C=0x%02X (Atteso 0x78)",
                    cpu.B.get(), cpu.C.get()
            ));
        }
    }

    /**
     * Telemetria per osservare la risalita dello stack e l'acquisizione dei singoli byte
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        int ramFc = mmu.readByte(0xFFFC);
        int ramFd = mmu.readByte(0xFFFD);

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-15s | SP: 0x%04X | BC: 0x%02X%02X | RAM[0xFFFC]: 0x%02X | RAM[0xFFFD]: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.SP.get(),
                cpu.B.get(), cpu.C.get(),
                ramFc,
                ramFd,
                cpu.getTotalTicks()
        );
    }
}