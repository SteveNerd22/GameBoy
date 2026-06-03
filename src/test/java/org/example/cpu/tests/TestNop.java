package org.example.cpu.tests;

import org.example.Main;
import org.example.cpu.CpuTestContext;
import org.example.cpu.CpuTestRunner;

public class TestNop {
    public static void main(String[] args) {
        System.out.println("====== TEST SCENARIO: NOP ======");
        if (args.length > 0) {
            Main.DEBUG = args[0].equalsIgnoreCase("debug");
        }

        CpuTestContext context = new CpuTestContext(new int[]{ 0x00 });

        CpuTestRunner.run(context);
    }
}