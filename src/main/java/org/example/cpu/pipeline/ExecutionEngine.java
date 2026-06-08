package org.example.cpu.pipeline;

import org.example.cpu.SM83;
import org.example.cpu.pipeline.instructions.CpuInstruction;
import org.example.cpu.pipeline.instructions.InstructionRegistry;
import org.example.cpu.pipeline.instructions.Opcode_Nop;

import java.lang.classfile.instruction.NopInstruction;

public class ExecutionEngine {

    private CpuInstruction currentInstruction = new Opcode_Nop();
    private int stateTicks;
    private long totalTicks;

    public ExecutionEngine() {
        this.stateTicks = 0;
        this.totalTicks = 0;
    }

    /**
     * Il cuore pulsante del ciclo macchina.
     * Esegue Execute e Fetch in parallelo secondo le regole del silicio SM83.
     */
    public void pulse(SM83 cpu, boolean isCBSet) {
        this.stateTicks++;
        this.totalTicks ++;

        if (this.stateTicks < 4) {
            return;
        }

        cpu.PC.emit();

        currentInstruction.execute(cpu);


        if (currentInstruction.isFinished()) {
            cpu.IR.sampleSoCBus();
            cpu.idu.incrementFormSoC();
            cpu.PC.sampleFromIduBus(); // PC = PC + 1

            int nextOpcode = cpu.IR.get();
            this.currentInstruction = InstructionRegistry.get(nextOpcode, isCBSet);
            cpu.controlUnit.resetCBPrefix();
            this.currentInstruction.prepare(nextOpcode);
        }

        this.stateTicks = 0;
    }

    public CpuInstruction getCurrentInstruction() {
        return this.currentInstruction;
    }

    public void reset() {
        this.stateTicks = 0;
        this.totalTicks = 0;
        this.currentInstruction = new Opcode_Nop();
    }

    public long getTotalTicks() {
        return this.totalTicks;
    }
}