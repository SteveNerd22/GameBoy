package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LoadIndirectHl implements CpuTestCase {

    // Esecuzione rapida e tracciata direttamente dall'IDE
    public static void main(String[] args) {
        int[] rom = {
                0x26, 0xC0, // LD H, 0xC0
                0x2E, 0x00, // LD L, 0x00 -> HL = 0xC000
                0x46,       // LD B, (HL) (Richiede 2 M-Cycles)
                0x48,       // LD C, B    (Richiede 1 M-Cycle)
                0x00,       // NOP
                0x00        // NOP
        };

        // Lanciamo il tracciamento standalone ereditato dall'interfaccia
        new Test_LoadIndirectHl().runAsPipelineTrace(rom, 9, gameBoy -> {
            SM83 cpu = gameBoy.getCpu();

            gameBoy.getMmu().writeByte(0xC000, 0x55);

            // Azzeriamo i registri coinvolti per garantire un test pulito
            cpu.B.set(0x00);
            cpu.C.set(0x00);
            cpu.H.set(0x00);
            cpu.L.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // Logica stabile per il TestSuiteRunner automatico
        int[] rom = {
                0x26, 0xC0,
                0x2E, 0x00,
                0x46,
                0x48
        };
        mmu.loadCartridge(rom);
        mmu.writeByte(0xC000, 0x55);

        cpu.reset();
        cpu.PC.set(0x0000);

        // 32 T-Ticks = 8 M-Cycles per arrivare al consolidamento del valore dentro il registro C
        for (int i = 0; i < 32; i++) {
            cpu.pulse();
        }

        // --- ASSERZIONI PER LA SUITE AUTOMATICA ---
        reporter.incrementAssertions();
        if (cpu.B.get() != 0x55) {
            reporter.reportFailure(0x46, "LD B, (HL) fallito: B non contiene 0x55");
        }

        reporter.incrementAssertions();
        if (cpu.C.get() != 0x55) {
            reporter.reportFailure(0x48, "Esecuzione concatenata fallita: C non contiene 0x55");
        }
    }

    /**
     * Sovrascriviamo il metodo di logging predefinito per personalizzare l'output
     * di questa specifica classe, includendo la visualizzazione accoppiata di HL.
     */
    @Override
    public void printStateLine(int mCycle, SM83 cpu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Boot/Fetch)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-19s | HL: 0x%02X%02X | B: 0x%02X | C: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.H.get(), cpu.L.get(),
                cpu.B.get(),
                cpu.C.get(),
                cpu.getTotalTicks()
        );
    }
}