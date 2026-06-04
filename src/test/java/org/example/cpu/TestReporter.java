package org.example.cpu;

import java.util.ArrayList;
import java.util.List;

public final class TestReporter {
    private int totalAssertions = 0;
    private final List<TestFailure> failures = new ArrayList<>();

    public void incrementAssertions() {
        this.totalAssertions++;
    }

    public void reportFailure(int opcode, String description) {
        failures.add(new TestFailure(opcode, description));
    }

    public int getTotalAssertions() { return totalAssertions; }
    public List<TestFailure> getFailures() { return failures; }
    public boolean hasFailed() { return !failures.isEmpty(); }
}

