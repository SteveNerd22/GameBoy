package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_CallAndReturn implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = new int[0x2000];

        // Configura il codice nel finto spazio ROM
        rom[0x1F74] = 0xCD; // CALL nn
        rom[0x1F75] = 0x61; // Parte bassa dell'indirizzo (0x61)
        rom[0x1F76] = 0x00; // Parte alta dell'indirizzo (0x00)
        // L'indirizzo di ritorno atteso sarà 0x1F77

        rom[0x0061] = 0xC9; // RET (Mettiamo subito il Return nella sub-routine)

        new Test_CallAndReturn().runAsPipelineTrace(rom, 15, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x1F74); // Partiamo dallo stesso identico punto di Pokémon Rosso
            cpu.SP.set(0xFFFE); // Stack pointer iniziale standard
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = new int[0x2000];
        rom[0x1F74] = 0xCD; rom[0x1F75] = 0x61; rom[0x1F76] = 0x00;
        rom[0x0061] = 0xC9;
        mmu.loadCartridge(rom);

        cpu.reset();
        cpu.PC.set(0x1F74);
        cpu.SP.set(0xFFFE);

        System.out.println("\n=== [TEST] INIZIO ESECUZIONE CALL ===");
        // Eseguiamo i cicli della CALL (Fetch + cicli interni dell'istruzione)
        // Di solito una CALL standard richiede circa 24 Ticks (6 M-Cycles) totali
        for (int i = 0; i < 24; i++) cpu.pulse();

        // Ispezione della RAM dello Stack dopo la CALL
        int byteBassoInRam = mmu.readByte(0xFFFC, cpu);
        int byteAltoInRam = mmu.readByte(0xFFFD, cpu);

        System.out.println("--- ISPEZIONE RAM STACK DOPO LA CALL ---");
        System.out.printf("Indirizzo RAM 0xFFFD (Atteso Byte Alto 0x1F): 0x%02X\n", byteAltoInRam);
        System.out.printf("Indirizzo RAM 0xFFFC (Atteso Byte Basso 0x77): 0x%02X\n", byteBassoInRam);
        System.out.printf("Stato attuale SP (Atteso 0xFFFC): 0x%04X\n", cpu.SP.get());
        System.out.printf("PC attuale (Atteso 0x0061): 0x%04X\n", cpu.PC.get());

        reporter.incrementAssertions();
        if (byteAltoInRam != 0x1F || byteBassoInRam != 0x77) {
            reporter.reportFailure(0xCD, "CALL non ha scritto l'indirizzo di ritorno corretto (0x1F77) in memoria!");
        }

        if (cpu.PC.get() != 0x0061) {
            reporter.reportFailure(0xCD, "La CALL non ha deviato il PC a 0x0061!");
        }

        System.out.println("\n=== [TEST] INIZIO ESECUZIONE RET ===");
        // Eseguiamo i cicli della RET (circa 16 Ticks / 4 M-Cycles)
        for (int i = 0; i < 16; i++) cpu.pulse();

        System.out.println("--- STATO DOPO LA RET ---");
        System.out.printf("Stato finale SP (Atteso ripristino a 0xFFFE): 0x%04X\n", cpu.SP.get());
        System.out.printf("PC Finale (Atteso ripristino a 0x1F77): 0x%04X\n", cpu.PC.get());

        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x1F77) {
            reporter.reportFailure(0xC9, String.format("RET fallita! Il PC è finito a 0x%04X invece di 0x1F77", cpu.PC.get()));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %-2d | PC: 0x%04X | IR: 0x%02X | Op: %-20s | SP: 0x%04X | RAM[0xFFFC/D]: [0x%02X, 0x%02X]\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.SP.get(),
                mmu.readByte(0xFFFC, cpu),
                mmu.readByte(0xFFFD, cpu)
        );
    }
}