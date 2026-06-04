package org.example.cpu;

import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.InterruptBus;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.mmu.MMU;

public class LoadChainPipelineTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   MULTIPLE CYCLES PIPELINE TRACE: SHARP SM83    ");
        System.out.println("==================================================");

        AddressBus addressBus = new AddressBus();
        DataBus dataBus = new DataBus();
        InterruptBus interruptBus = new InterruptBus();
        MMU mmu = new MMU(addressBus, dataBus);

        SM83 cpu = new SM83(interruptBus, dataBus, addressBus);
        ExecutionEngine engine = cpu.getExecutionEngine();

        // Generiamo il programma nella ROM virtuale
        int[] isolatedRom = new int[0x4000];

        isolatedRom[0x0000] = 0x06; isolatedRom[0x0001] = 0x99; // LD B, 0x99
        isolatedRom[0x0002] = 0x48; // LD C, B

        mmu.loadCartridge(isolatedRom);

        // Setup dello Stato Hardware Iniziale
        cpu.reset();
        engine.reset();
        cpu.PC.set(0x0000);
        cpu.F.set(0x00);

        // Questa volta non pre-carichiamo B a mano! Sarà la prima istruzione a farlo dalla memoria.
        cpu.B.set(0x00);

        System.out.println("Stato iniziale (Boot - Catena Vuota):");
        printStateLine(0, cpu, engine, engine.getCurrentInstruction().getClass().getSimpleName());
        System.out.println("---------------------------------------------------------------------------------------");

        // Facciamo girare la CPU per 7 M-Cycles totali per osservare lo stallo positivo del bus
        int targetMCycles = 4;
        for (int mCycle = 1; mCycle <= targetMCycles; mCycle++) {

            // Consumiamo 1 M-Cycle (4 T-Ticks)
            for (int tick = 0; tick < 4; tick++) {
                cpu.pulse();
            }

            // Recuperiamo informazioni sull'istruzione corrente
            String currentOpName = "None";
            if (engine.getCurrentInstruction() != null) {
                currentOpName = engine.getCurrentInstruction().getClass().getSimpleName();
            }

            printStateLine(mCycle, cpu, engine, currentOpName);
        }

        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("Fine della traccia multi-ciclo.");
        System.out.println("==================================================");
    }

    private static void printStateLine(int mCycle, SM83 cpu, ExecutionEngine engine, String opName) {
        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-15s | B: 0x%02X | C: 0x%02X | D: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                opName,
                cpu.B.get(),
                cpu.C.get(), // Adatta in base al tuo metodo di get dei registri
                cpu.D.get(),
                cpu.getTotalTicks()
        );
    }
}