package org.example.cpu.tests.cases;

import org.example.cpu.Register;
import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_LdRegReg implements CpuTestCase {

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        Register[] registers = { cpu.B, cpu.C, cpu.D, cpu.E, cpu.H, cpu.L, cpu.A };
        int[] hardwareIds =    {   0,     1,     2,     3,     4,     5,     7  }; // Notare il salto da 5 a 7!
        String[] regNames =    {  "B",   "C",   "D",   "E",   "H",   "L",   "A" };

        for (int i = 0; i < registers.length; i++) {
            for (int j = 0; j < registers.length; j++) {

                Register dest = registers[i];
                Register src = registers[j];

                String destName = regNames[i];
                String srcName = regNames[j];

                // Calcoliamo l'opcode usando gli ID hardware corretti della CPU
                int opcode = 0x40 | (hardwareIds[i] << 3) | hardwareIds[j];

                // PROTEZIONE DI SICUREZZA: Se per qualsiasi motivo la formula genera 0x76 (HALT), lo saltiamo.
                if (opcode == 0x76) {
                    continue;
                }

                // 1. Setup dello stato iniziale controllato
                cpu.reset();
                cpu.PC.set(0x0000);
                cpu.F.set(0x00);

                int expectedValue = 0x20 + j;
                src.set(expectedValue);

                if (dest != src) {
                    dest.set(0x00);
                }

                int[] fakeCartridge = new int[0x4000];
                fakeCartridge[0x0000] = opcode;
                mmu.loadCartridge(fakeCartridge);

                long startTicks = cpu.getTotalTicks();

                // Facciamo girare la CPU per gli 8 tick (M1 Fetch + M2 Execute sovrapposti)
                for (int tick = 0; tick < 8; tick++) {
                    cpu.pulse();
                }

                long elapsedTicks = cpu.getTotalTicks() - startTicks;

                // --- ASSERZIONI ---
                reporter.incrementAssertions();
                if (dest.get() != expectedValue) {
                    reporter.reportFailure(opcode, String.format(
                            "LD %s, %s fallito. Il registro di destinazione contiene 0x%02X, atteso: 0x%02X",
                            destName, srcName, dest.get(), expectedValue
                    ));
                }

                reporter.incrementAssertions();
                if (cpu.PC.get() != 0x0002) {
                    reporter.reportFailure(opcode, String.format(
                            "PC disallineato per LD %s, %s. Trovato: 0x%04X, atteso: 0x0002",
                            destName, srcName, cpu.PC.get()
                    ));
                }

                // NOTA: il test calcola i tick totali spesi dall'esecuzione (8 tick in questo ciclo di catena)
                reporter.incrementAssertions();
                if (elapsedTicks != 8) {
                    reporter.reportFailure(opcode, String.format(
                            "Timing errato per LD %s, %s. Ticks spesi: %d, attesi: 8",
                            destName, srcName, elapsedTicks
                    ));
                }

                reporter.incrementAssertions();
                if (cpu.F.get() != 0x00) {
                    reporter.reportFailure(opcode, String.format(
                            "I flag in F sono stati alterati corrompendo lo stato durante LD %s, %s. F = 0x%02X",
                            destName, srcName, cpu.F.get()
                    ));
                }
            }
        }
    }
}