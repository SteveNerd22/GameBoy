package org.example.cpu.tests;

import org.example.Main;
import org.example.cpu.CpuTestContext;
import org.example.cpu.CpuTestRunner;

public class TestStoreToRam {
    public static void main(String[] args) {
        System.out.println("====== TEST SCENARIO: LD (HL), r ======");
        if (args.length > 0) {
            Main.DEBUG = args[0].equalsIgnoreCase("debug");
        }

        CpuTestContext context = new CpuTestContext(new int[]{ 0x3E, 0x99, 0x77 })
                .setRegister("HL", 0xC000); // Indirizzo RAM di destinazione

        CpuTestRunner.run(context);
    }
}