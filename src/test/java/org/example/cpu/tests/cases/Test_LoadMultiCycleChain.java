package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LoadMultiCycleChain implements CpuTestCase {

    // Abilitiamo il run al volo dall'IDE tramite il tasto Play sul main
    public static void main(String[] args) {
        int[] rom = {
                0x06, 0x99, // LD B, 0x99
                0x48        // LD C, B
        };

        // Utilizziamo il motore di tracciamento ereditato dall'interfaccia default
        new Test_LoadMultiCycleChain().runAsPipelineTrace(rom, 4, gameBoy -> {
            SM83 cpu = gameBoy.getCpu();
            cpu.B.set(0x00); // Forziamo B a zero per verificare la lettura reale della ROM
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica per il test suite automatico (verifica dei valori finali attesi)
        int[] rom = { 0x06, 0x99, 0x48 };
        mmu.loadCartridge(rom);
        cpu.reset();
        cpu.PC.set(0x0000);

        // 12 T-Ticks = 3 M-Cycles (1: Fetch LD, 2: Lettura 0x99, 3: Execute LD C,B + Fetch)
        for (int i = 0; i < 16; i++) {
            cpu.pulse();
        }

        reporter.incrementAssertions();
        if (cpu.C.get() != 0x99) {
            reporter.reportFailure(0x48, "Catena multi-ciclo fallita: C non contiene 0x99");
        }
    }
}