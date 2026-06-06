package org.example.cpu.tests.cases;

import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_DecimalAdjust implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = {
                0x27, // M1: DAA dopo somma 9+1 (A=0x0A, N=0, H=0, C=0) -> Atteso: A=0x10, Flags=[----]
                0x27, // M2: DAA dopo somma overflow (A=0x9A, N=0, H=0, C=0) -> Atteso: A=0x00, Flags=[Z--C]
                0x27, // M3: DAA dopo sottrazione (A=0x2A, N=1, H=1, C=0) -> Atteso: A=0x24, Flags=[-N--]
                0x00  // M4: NOP
        };

        new Test_DecimalAdjust().runAsPipelineTrace(rom, 4, gb -> {
            SM83 cpu = gb.getCpu();
            // Prepariamo lo stato per il primo DAA (risultato di 9 + 1 esadecimale = 0x0A)
            cpu.A.set(0x0A);
            cpu.F.set(0x00);
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = { 0x27, 0x27, 0x27, 0x00 };
        mmu.loadCartridge(rom);

        // --- Passo 1: Correzione semplice dopo Somma ---
        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.A.set(0x0A);
        cpu.F.set(0x00);

        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x10) {
            reporter.reportFailure(0x27, String.format("DAA Caso 1 fallito: A è 0x%02X, atteso 0x10", cpu.A.get()));
        }
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != 0x00) {
            reporter.reportFailure(0x27, String.format("DAA Caso 1 Flag falliti: F=0x%02X, atteso 0x00 [----]", cpu.F.get()));
        }

        // --- Passo 2: Overflow decimale con Carry ---
        // Iniettiamo i valori per il secondo DAA simulando l'M-Cycle precedente
        cpu.A.set(0x9A);
        cpu.F.set(0x00);

        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0x27, String.format("DAA Caso 2 fallito: A è 0x%02X, atteso 0x00", cpu.A.get()));
        }
        int expectedFlags2 = FlagsRegister.MASK_Z | FlagsRegister.MASK_C;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags2) {
            reporter.reportFailure(0x27, String.format("DAA Caso 2 Flag falliti: F=0x%02X, atteso 0x%02X [Z--C]", cpu.F.get(), expectedFlags2));
        }

        // --- Passo 3: Aggiustamento Sottrazione ---
        // Simuliamo una sottrazione che ha lasciato un valore non-BCD e Half-Carry attivo
        cpu.A.set(0x2A);
        cpu.F.set(FlagsRegister.MASK_N | FlagsRegister.MASK_H);

        for (int i = 0; i < 4; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.A.get() != 0x24) {
            reporter.reportFailure(0x27, String.format("DAA Caso 3 fallito: A è 0x%02X, atteso 0x24", cpu.A.get()));
        }
        int expectedFlags3 = FlagsRegister.MASK_N;
        reporter.incrementAssertions();
        if ((cpu.F.get() & 0xF0) != expectedFlags3) {
            reporter.reportFailure(0x27, String.format("DAA Caso 3 Flag falliti: F=0x%02X, atteso 0x%02X [-N--]", cpu.F.get(), expectedFlags3));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-22s | A: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.A.get(),
                getFlagsString(cpu.F.get()),
                cpu.getTotalTicks()
        );
    }

    private String getFlagsString(int f) {
        return String.format("[%s%s%s%s]",
                (f & FlagsRegister.MASK_Z) != 0 ? "Z" : "-",
                (f & FlagsRegister.MASK_N) != 0 ? "N" : "-",
                (f & FlagsRegister.MASK_H) != 0 ? "H" : "-",
                (f & FlagsRegister.MASK_C) != 0 ? "C" : "-"
        );
    }
}