package org.example.cpu.pipeline;

import org.example.cpu.SM83;
import org.example.cpu.pipeline.instructions.CpuInstruction;
import org.example.cpu.pipeline.instructions.InstructionRegistry;

public class ExecutionEngine {

    private PipelineStep currentStep;
    private CpuInstruction currentInstruction;
    private int stateTicks;

    public ExecutionEngine() {
        this.currentStep = PipelineStep.FETCH;
        this.stateTicks = 0;
    }

    /**
     * Il ciclo principale centralizzato.
     * La logica di esecuzione è qui, ma le regole di transizione appartengono all'enum.
     */
    public void pulse(SM83 cpu) {
        this.stateTicks++;

        if (this.stateTicks < 4) {
            return;
        }

        if (this.currentInstruction == null || this.currentInstruction.isFinished()) {
            int currentOpcode = cpu.IR.get();
            this.currentInstruction = InstructionRegistry.get(currentOpcode);
            this.currentInstruction.prepare(currentOpcode);
        }

        this.currentInstruction.execute(cpu);

        if (this.currentInstruction.isFinished()) {

            cpu.PC.emit();
            cpu.idu.incrementFormSoC();
            cpu.PC.sampleFromIduBus(); // PC = PC + 1

            cpu.IR.sampleSoCBus(); // IR = (PC)
        }

        this.stateTicks = 0;
    }

    CpuInstruction getCurrentInstruction() {
        return this.currentInstruction;
    }

    public PipelineStep getCurrentStage() {
        return this.currentStep;
    }
}