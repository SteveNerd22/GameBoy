package org.example.cpu;

public class CpuPipelineTest {

    public static void main(String[] args) {
        System.out.println("============== SUITE DI TEST CPU PIPELINE ==============");

        // --- TEST 1: Solo NOP ---
        System.out.println("\n[SCENARIO 1: ESECUZIONE NOP]");
        CpuTestContext testNop = new CpuTestContext(new int[]{ 0x00 });
        CpuTestRunner.run(testNop);


        // --- TEST 2: LD A, n ---
        System.out.println("\n[SCENARIO 2: CARICAMENTO IMMEDIATO IN A]");
        CpuTestContext testLdImmediate = new CpuTestContext(new int[]{ 0x3E, 0x42 });
        CpuTestRunner.run(testLdImmediate);


        // --- TEST 3: LD (HL), r (Scrittura in RAM) ---
        System.out.println("\n[SCENARIO 3: SCRITTURA INDIRETTA IN RAM VIA HL]");
        CpuTestContext testStoreToRam = new CpuTestContext(new int[]{ 0x3E, 0x99, 0x77 })
                .setRegister("HL", 0xC000); // Prepariamo l'indirizzo di destinazione
        CpuTestRunner.run(testStoreToRam);


        // --- TEST 4: LD r, (HL) (Lettura da RAM) ---
        System.out.println("\n[SCENARIO 4: LETTURA INDIRETTA DA RAM VIA HL]");
        CpuTestContext testLoadFromRam = new CpuTestContext(new int[]{ 0x46 }) // LD B, (HL)
                .setRegister("HL", 0xC000)
                .writeRam(0xC000, 0x55); // Prepariamo il dato esatto nella RAM del SoC
        CpuTestRunner.run(testLoadFromRam);
    }
}