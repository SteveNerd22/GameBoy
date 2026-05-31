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

        switch (this.currentStep) {
            case FETCH:
                cpu.PC.emitAddress();
                cpu.idu.increment(cpu.PC.get());

                int opcode = cpu.SoCData.sampleByte();
                cpu.IR.setValue(opcode);
                break;

            case DECODE:
                int currentOpcode = cpu.IR.get();
                this.currentInstruction = InstructionRegistry.get(currentOpcode);
                this.currentInstruction.setTriggeredOpcode(currentOpcode);
                this.currentInstruction.prepare();
                break;

            case EXECUTE:
                if (this.currentInstruction != null) {
                    this.currentInstruction.executeCycle(cpu);
                }
                break;
        }

        if (this.currentStep.shouldTransition(this.stateTicks, this)) {
            this.currentStep = this.currentStep.getNext(cpu, this);
            this.stateTicks = 0;
        }
    }

    // API interna per permettere all'enum di ispezionare l'istruzione in EXECUTE
    CpuInstruction getCurrentInstruction() {
        return this.currentInstruction;
    }

    public PipelineStep getCurrentStage() {
        return this.currentStep;
    }
}