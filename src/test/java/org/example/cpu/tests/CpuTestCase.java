package org.example.cpu.tests;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.mmu.MMU;

public interface CpuTestCase {
    /**
     * Restituisce il nome identificativo del test (es. "INC (HL) & DEC (HL)")
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Esegue la suite di test specifica per una classe di opcode.
     * Ha pieno controllo sulla CPU e può eseguire più scenari di test al suo interno.
     */
    void execute(SM83 cpu, MMU mmu, TestReporter reporter);
}