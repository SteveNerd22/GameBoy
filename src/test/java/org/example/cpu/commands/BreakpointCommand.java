package org.example.cpu.commands;

import org.example.cpu.DebugCommand;
import org.example.cpu.DebugContext;

public class BreakpointCommand implements DebugCommand {
    @Override public String getKeyword() { return "bp"; }
    @Override public String getHelp() { return "Imposta un breakpoint PC (es: bp C000)"; }

    @Override
    public boolean execute(DebugContext ctx, String[] args) {
        if (args.length < 1) {
            System.out.println("⚠️ Specifica un indirizzo esadecimale.");
            return false;
        }
        try {
            int addr = Integer.parseInt(args[0].replace("0x", ""), 16);
            ctx.addBreakpoint(addr);
            System.out.printf("🛑 Breakpoint impostato a 0x%04X\n", addr);
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Indirizzo non valido.");
        }
        return false;
    }
}