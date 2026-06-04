package org.example.cpu.tests.standalone;

import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.InterruptBus;
import org.example.cpu.SM83;
import org.example.cpu.pipeline.ExecutionEngine;
import org.example.mmu.MMU;

public class LoadIndirectHlPipelineTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  INDIRECT MEMORY (HL) PIPELINE TRACE: SHARP SM83 ");
        System.out.println("==================================================");

        AddressBus addressBus = new AddressBus();
        DataBus dataBus = new DataBus();
        InterruptBus interruptBus = new InterruptBus();
        MMU mmu = new MMU(addressBus, dataBus);

        SM83 cpu = new SM83(interruptBus, dataBus, addressBus);
        ExecutionEngine engine = cpu.getExecutionEngine();

        // 1. Generiamo il programma nella ROM virtuale
        int[] isolatedRom = new int[0x4000];

        // Prepariamo i registri H e L usando LD immediate (Opcode 0x26 e 0x2E)
        isolatedRom[0x0000] = 0x26; isolatedRom[0x0001] = 0xC0; // LD H, 0xC0
        isolatedRom[0x0002] = 0x2E; isolatedRom[0x0003] = 0x00; // LD L, 0x00 -> HL = 0xC000

        // L'istruzione sotto lente d'ingrandimento: LD B, (HL)
        isolatedRom[0x0004] = 0x46;                              // LD B, (HL) (Prende 2 M-Cycles)

        // Verifica di passaggio nella catena
        isolatedRom[0x0005] = 0x48;                              // LD C, B    (Prende 1 M-Cycle)
        isolatedRom[0x0006] = 0x00;                              // NOP
        isolatedRom[0x0007] = 0x00;                              // NOP

        mmu.loadCartridge(isolatedRom);

        // 2. Prepariamo la RAM (Work RAM) scrivendo il valore di test all'indirizzo puntato da HL
        // Usiamo writeByte direttamente per simulare una RAM pre-popolata da hardware/gioco
        mmu.writeByte(0xC000, 0x55);

        // 3. Setup dello Stato Hardware Iniziale della CPU
        cpu.reset();
        engine.reset();
        cpu.PC.set(0x0000);
        cpu.F.set(0x00);
        cpu.B.set(0x00);
        cpu.C.set(0x00);
        cpu.H.set(0x00);
        cpu.L.set(0x00);

        System.out.println("Stato iniziale (Boot - Catena Vuota):");
        printStateLine(0, cpu, engine, engine.getCurrentInstruction().getClass().getSimpleName());
        System.out.println("-------------------------------------------------------------------------------------------------");

        // Eseguiamo il ciclo per 9 M-Cycles totali per vedere l'intera evoluzione della memoria
        int targetMCycles = 9;
        for (int mCycle = 1; mCycle <= targetMCycles; mCycle++) {

            // Consumiamo 1 M-Cycle (4 T-Ticks)
            for (int tick = 0; tick < 4; tick++) {
                cpu.pulse();
            }

            String currentOpName = "None";
            if (engine.getCurrentInstruction() != null) {
                currentOpName = engine.getCurrentInstruction().getClass().getSimpleName();
            }

            printStateLine(mCycle, cpu, engine, currentOpName);
        }

        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.println("Fine della traccia memoria indiretta.");
        System.out.println("==================================================");
    }

    private static void printStateLine(int mCycle, SM83 cpu, ExecutionEngine engine, String opName) {
        System.out.printf(
                "M-Cycle: %d | PC: 0x%04X | IR: 0x%02X | Active Op: %-18s | HL: 0x%02X%02X | B: 0x%02X | C: 0x%02X | Ticks: %d\n",
                mCycle,
                cpu.PC.get(),
                cpu.IR.get(),
                opName,
                cpu.H.get(), cpu.L.get(),
                cpu.B.get(),
                cpu.C.get(),
                cpu.getTotalTicks()
        );
    }
}