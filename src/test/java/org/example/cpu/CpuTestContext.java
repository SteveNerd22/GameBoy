package org.example.cpu;

import java.util.HashMap;
import java.util.Map;

public class CpuTestContext {
    private final int[] program;
    private final Map<String, Integer> initialRegisters = new HashMap<>();
    private final Map<Integer, Integer> initialRam = new HashMap<>();

    public CpuTestContext(int[] program) {
        this.program = program;
    }

    public CpuTestContext setRegister(String name, int value) {
        initialRegisters.put(name.toUpperCase(), value);
        return this;
    }

    public CpuTestContext writeRam(int address, int value) {
        initialRam.put(address, value);
        return this;
    }

    // Getters per il TestRunner
    public int[] getProgram() { return program; }
    public Map<String, Integer> getInitialRegisters() { return initialRegisters; }
    public Map<Integer, Integer> getInitialRam() { return initialRam; }
}