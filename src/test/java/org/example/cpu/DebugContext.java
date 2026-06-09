package org.example.cpu;

import org.example.GameBoy;
import java.util.*;

public class DebugContext {
    private final GameBoy gameBoy;
    private boolean running = true;
    private final Set<Integer> pcBreakpoints = new HashSet<>();
    private final LinkedList<String> executionHistory = new LinkedList<>();
    private final int MAX_HISTORY = 50; // Ricorda le ultime 50 istruzioni

    public DebugContext(GameBoy gameBoy) {
        this.gameBoy = gameBoy;
    }

    public GameBoy getGameBoy() { return gameBoy; }
    public SM83 getCpu() { return gameBoy.getCpu(); }

    public boolean isRunning() { return running; }
    public void stop() { this.running = false; }

    // Gestione Breakpoint
    public void addBreakpoint(int address) { pcBreakpoints.add(address); }
    public void removeBreakpoint(int address) { pcBreakpoints.remove(address); }
    public boolean shouldBreakAt(int address) { return pcBreakpoints.contains(address); }

    // Gestione Cronologia Log
    public void addLogEntry(String log) {
        if (executionHistory.size() >= MAX_HISTORY) {
            executionHistory.removeFirst();
        }
        executionHistory.addLast(log);
    }
    public List<String> getHistory() { return executionHistory; }
}