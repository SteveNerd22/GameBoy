package org.example.cpu.commands;

import org.example.cpu.DebugCommand;
import org.example.cpu.DebugContext;
import org.example.cpu.SM83;

public class RegCommand implements DebugCommand {
    @Override public String getKeyword() { return "reg"; }
    @Override public String getHelp() { return "Mostra lo stato corrente dei registri CPU"; }

    @Override
    public boolean execute(DebugContext ctx, String[] args) {
        SM83 cpu = ctx.getCpu();
        System.out.println("\n--- 🧠 STATO REALE CPU ---");
        System.out.printf("PC: 0x%04X  |  SP: 0x%04X  |  IR: 0x%02X\n", cpu.PC.get(), cpu.SP.get(), cpu.IR.get());
        System.out.printf("A:  0x%02X    |  F:  0x%02X (%s)\n", cpu.A.get(), cpu.F.get(), getFlagsString(cpu.F.get()));
        System.out.printf("B:  0x%02X    |  C:  0x%02X   => BC: 0x%04X\n", cpu.B.get(), cpu.C.get(), (cpu.B.get() << 8) | cpu.C.get());
        System.out.printf("D:  0x%02X    |  E:  0x%02X   => DE: 0x%04X\n", cpu.D.get(), cpu.E.get(), (cpu.D.get() << 8) | cpu.E.get());
        System.out.printf("H:  0x%02X    |  L:  0x%02X   => HL: 0x%04X\n", cpu.H.get(), cpu.L.get(), cpu.HL.get());
        System.out.println("---------------------------\n");
        return false; // Non avanza il tempo
    }

    private String getFlagsString(int f) {
        return String.format("[%s%s%s%s]",
                (f & 0x80) != 0 ? "Z" : "-", (f & 0x40) != 0 ? "N" : "-", (f & 0x20) != 0 ? "H" : "-", (f & 0x10) != 0 ? "C" : "-");
    }
}