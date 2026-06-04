package org.example.cpu.tests.cases;

import org.example.cpu.Register;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdRegReg implements CpuTestCase {

    // Vuoi vedere come si comporta nello specifico LD C, B (0x48) a livello di clock? Premi Play!
    public static void main(String[] args) {
        int[] singleOpcodeRom = { 0x48 }; // LD C, B

        new Test_LdRegReg().runAsPipelineTrace(singleOpcodeRom, 2, gameBoy -> {
            SM83 cpu = gameBoy.getCpu();
            cpu.B.set(0x42); // Configurazione preliminare del registro sorgente
            cpu.C.set(0x00); // Reset della destinazione
        });
    }

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        Register[] registers = { cpu.B, cpu.C, cpu.D, cpu.E, cpu.H, cpu.L, cpu.A };
        int[] hardwareIds =    {   0,     1,     2,     3,     4,     5,     7  };
        String[] regNames =    {  "B",   "C",   "D",   "E",   "H",   "L",   "A" };

        for (int i = 0; i < registers.length; i++) {
            for (int j = 0; j < registers.length; j++) {
                Register dest = registers[i];
                Register src = registers[j];
                String destName = regNames[i];
                String srcName = regNames[j];

                int opcode = 0x40 | (hardwareIds[i] << 3) | hardwareIds[j];

                if (opcode == 0x76) continue; // Salta HALT

                cpu.reset();
                cpu.PC.set(0x0000);
                cpu.F.set(0x00);

                int expectedValue = 0x20 + j;
                src.set(expectedValue);
                if (dest != src) dest.set(0x00);

                int[] fakeCartridge = new int[0x4000];
                fakeCartridge[0x0000] = opcode;
                mmu.loadCartridge(fakeCartridge);

                long startTicks = cpu.getTotalTicks();
                for (int tick = 0; tick < 8; tick++) {
                    cpu.pulse();
                }
                long elapsedTicks = cpu.getTotalTicks() - startTicks;

                // Asserzioni stabili per il TestSuiteRunner
                reporter.incrementAssertions();
                if (dest.get() != expectedValue) {
                    reporter.reportFailure(opcode, String.format("LD %s, %s fallito.", destName, srcName));
                }
                reporter.incrementAssertions();
                if (cpu.PC.get() != 0x0002) { // Confermato 0x0002 per via del secondo fetch in catena
                    reporter.reportFailure(opcode, String.format("PC disallineato per LD %s, %s.", destName, srcName));
                }
                reporter.incrementAssertions();
                if (elapsedTicks != 8) {
                    reporter.reportFailure(opcode, String.format("Timing errato per LD %s, %s.", destName, srcName));
                }
                reporter.incrementAssertions();
                if (cpu.F.get() != 0x00) {
                    reporter.reportFailure(opcode, String.format("Flag alterati in LD %s, %s.", destName, srcName));
                }
            }
        }
    }
}