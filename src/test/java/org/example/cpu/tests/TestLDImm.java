package org.example.cpu.tests;

import org.example.Main;
import org.example.cpu.CpuTestContext;
import org.example.cpu.CpuTestRunner;

public class TestLDImm {
    public static void main(String[] args) {
        System.out.println("====== TEST SCENARIO: LD A, n ======");
        if (args.length > 0) {
            Main.DEBUG = args[0].equalsIgnoreCase("debug");
        }

        CpuTestContext context = new CpuTestContext(new int[]{ 0x3E, 0x42 });

        CpuTestRunner.run(context);
    }
}