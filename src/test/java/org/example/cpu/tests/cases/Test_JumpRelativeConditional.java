package org.example.cpu.tests.cases;

import org.example.Main;
import org.example.cpu.FlagsRegister;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_JumpRelativeConditional implements CpuTestCase {

    public static void main(String[] args) {
        int[] rom = new int[0x200];

        // Caso 1: Z = 0 -> Condizione Fallita (Esce al primo ciclo utile)
        rom[0x0000] = 0x28; // JR Z, e
        rom[0x0001] = 0x05; // Offset +5 (ignorato)
        rom[0x0002] = 0x00; // Prossima istruzione dritto per dritto

        // Caso 2: Z = 1 -> Condizione Vera (Salta e propaga i bus)
        rom[0x0010] = 0x28; // JR Z, e
        rom[0x0011] = 0x04; // Offset +4. Destinazione attesa: 0x0012 + 4 = 0x0016
        rom[0x0016] = 0x00; // NOP di atterraggio

        // Tracciamo i cicli per vedere la pipeline all'opera su entrambi i rami
        new Test_JumpRelativeConditional().runAsPipelineTrace(rom, 8, gb -> {
            SM83 cpu = gb.getCpu();
            cpu.reset();
            cpu.PC.set(0x0000);
            cpu.F.set(0x00); // Forziamo Z = 0 per il primo ramo
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        int[] rom = new int[0x200];
        rom[0x0000] = 0x28; rom[0x0001] = 0x05; rom[0x0002] = 0x00;
        rom[0x0010] = 0x28; rom[0x0011] = 0x04; rom[0x0016] = 0x00;
        mmu.loadCartridge(rom);

        // --- TEST 1: Condizione FALSA (Z = 0) ---
        cpu.reset();
        cpu.PC.set(0x0000);
        cpu.F.set(0x00); // Zero flag spento, innesca l'uscita anticipata al case 0

        // 1 M-Cycle per il Fetch dell'opcode + 1 M-Cycle per l'esecuzione del case 0 (Totale 8 Ticks)
        for (int i = 0; i < 12; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x0002) {
            reporter.reportFailure(0x28, String.format("JR Z (Falso) fallito: PC è a 0x%04X, atteso 0x0002", cpu.PC.get()));
        }

        // --- TEST 2: Condizione VERA (Z = 1) ---
        cpu.reset();
        cpu.PC.set(0x0010);
        cpu.F.set(FlagsRegister.MASK_Z); // Zero flag acceso, esegue tutti i case dello switch

        // 1 M-Cycle Fetch + 3 M-Cycles di step (case 0, 1, 2) -> Totale 4 M-Cycles (16 Ticks)
        for (int i = 0; i < 16; i++) cpu.pulse();

        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x0016) {
            reporter.reportFailure(0x28, String.format("JR Z (Vero) fallito: PC è a 0x%04X, atteso 0x0016", cpu.PC.get()));
        }
    }

    @Override
    public void printStateLine(int mCycle, SM83 cpu, MMU mmu, ExecutionEngine engine) {
        String currentOpName = (engine.getCurrentInstruction() != null)
                ? engine.getCurrentInstruction().getClass().getSimpleName()
                : "None (Fetch/Overlap)";

        // Hack dinamico per invertire lo stato del flag Z nel mezzo della traccia del main
        // in modo da mostrare sia il comportamento del salto fallito che di quello riuscito.
        if (mCycle == 4 && cpu.PC.get() == 0x0004) {
            cpu.PC.set(0x0010);
            cpu.F.set(FlagsRegister.MASK_Z);
            System.out.println("--- [Traccia] Cambio contesto: Passaggio a PC 0x0010 con Z=1 ---");
        }

        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-30s | WZ: 0x%04X | Z: 0x%02X | Flags: %s | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                currentOpName,
                cpu.WZ.get(),
                cpu.Z.get(),
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