package org.example.cpu.tests;

import org.example.Main;
import org.example.cpu.CpuTestContext;
import org.example.cpu.CpuTestRunner;

public class TestLoadFromRam {
    public static void main(String[] args) {
        System.out.println("====== TEST SCENARIO: LD r, (HL) ======");
        if (args.length > 0) {
            Main.DEBUG = args[0].equalsIgnoreCase("debug");
        }

        CpuTestContext context = new CpuTestContext(new int[]{ 0x46 }) // LD B, (HL)
                .setRegister("HL", 0xC000)
                .writeRam(0xC000, 0x55); // Prepariamo il dato in RAM da leggere

        CpuTestRunner.run(context);
    }
}