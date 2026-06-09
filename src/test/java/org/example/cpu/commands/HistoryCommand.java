package org.example.cpu.commands;

import org.example.cpu.DebugCommand;
import org.example.cpu.DebugContext;

public class HistoryCommand implements DebugCommand {
    @Override public String getKeyword() { return "history"; }
    @Override public String getHelp() { return "Mostra la cronologia degli ultimi cicli eseguiti"; }

    @Override
    public boolean execute(DebugContext ctx, String[] args) {
        System.out.println("\n--- 📜 CRONOLOGIA ESECUZIONE ---");
        for (String log : ctx.getHistory()) {
            System.out.println(log);
        }
        System.out.println("--------------------------------\n");
        return false;
    }
}