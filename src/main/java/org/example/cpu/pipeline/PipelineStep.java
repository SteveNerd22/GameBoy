package org.example.cpu.pipeline;

import org.example.cpu.SM83;
import org.example.cpu.pipeline.instructions.CpuInstruction;

public enum PipelineStep {

    FETCH(1) {
        @Override
        public PipelineStep getNext(SM83 cpu, ExecutionEngine engine) {
            if (cpu.IR.get() == 0x00) {
                return FETCH;
            }
            return DECODE;
        }

        @Override
        public boolean shouldTransition(int currentTicks, ExecutionEngine engine) {
            return currentTicks >= this.requiredCycles;
        }
    },

    DECODE(1) {
        @Override
        public PipelineStep getNext(SM83 cpu, ExecutionEngine engine) {
            return EXECUTE;
        }

        @Override
        public boolean shouldTransition(int currentTicks, ExecutionEngine engine) {
            return currentTicks >= this.requiredCycles;
        }
    },

    EXECUTE(0) { // 0 significa dinamico
        @Override
        public PipelineStep getNext(SM83 cpu, ExecutionEngine engine) {
            return FETCH;
        }

        @Override
        public boolean shouldTransition(int currentTicks, ExecutionEngine engine) {
            CpuInstruction currentOpcode = engine.getCurrentInstruction();
            return currentOpcode == null || currentOpcode.isFinished();
        }
    };
    protected final int requiredCycles;

    PipelineStep(int requiredCycles) {
        this.requiredCycles = requiredCycles;
    }

    /**
     * Calcola polimorficamente qual è il prossimo stato in base al contesto della CPU.
     */
    public abstract PipelineStep getNext(SM83 cpu, ExecutionEngine engine);

    /**
     * Logica incorporata nell'enum per capire se è il momento di avanzare nella pipeline.
     */
    public abstract boolean shouldTransition(int currentTicks, ExecutionEngine engine);
}