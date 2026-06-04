package org.example.cpu.tests.cases;

import org.example.cpu.SM83;
import org.example.cpu.TestReporter;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

public class Test_Nop implements CpuTestCase {

    @Override
    public void execute(SM83 cpu, MMU mmu, TestReporter reporter) {
        // ==================================================
        // SCENARIO 1: Esecuzione di un singolo NOP (4 T-Ticks)
        // ==================================================

        // 1. Setup dello stato iniziale pulito
        cpu.reset();

        cpu.PC.set(0x0000);
        cpu.F.set(0x00);
        cpu.A.set(0x00);

        mmu.writeByte(0x0000, 0x00);

        long startTicks = cpu.getTotalTicks();

        // 2. Invece di un loop indefinito, spariamo esattamente 4 fronti di clock (1 M-Cycle)
        for (int i = 0; i < 4; i++) {
            cpu.pulse();
        }

        long elapsedTicks = cpu.getTotalTicks() - startTicks;

        // 3. VERIFICHE hardware basate sui registri dello stato finale

        // Verifica PC: L'istruzione è lunga 1 byte, quindi il PC deve essere tassativamente a 0x0001
        reporter.incrementAssertions();
        if (cpu.PC.get() != 0x0001) {
            reporter.reportFailure(0x00, String.format(
                    "Avanzamento PC errato dopo 4 impulsi. Atteso: 0x0001, Trovato: 0x%04X", cpu.PC.get()
            ));
        }

        // Verifica Timing: Verifichiamo che la funzione getTotalTicks funzioni nel contesto
        reporter.incrementAssertions();
        if (elapsedTicks != 4) {
            reporter.reportFailure(0x00, "Il contatore dei tick della CPU non ha registrato i 4 impulsi.");
        }

        // Verifica Registri: Un NOP non deve toccare assolutamente l'accumulatore
        reporter.incrementAssertions();
        if (cpu.A.get() != 0x00) {
            reporter.reportFailure(0x00, "Effetto collaterale dannoso: il registro A è cambiato dopo un NOP!");
        }

        // Verifica Flag: Un NOP non deve alterare il registro F
        reporter.incrementAssertions();
        if (cpu.F.get() != 0x00) {
            reporter.reportFailure(0x00, "Effetto collaterale dannoso: i flag in F sono cambiati dopo un NOP!");
        }
    }
}