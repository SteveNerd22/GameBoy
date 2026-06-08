package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

import java.lang.classfile.Opcode;

@CpuOpcode(value = {0xCD})
public final class Opcode_CallImmediate16 extends CpuInstruction{

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.PC.emit();
                cpu.Z.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 1 -> {
                cpu.PC.emit();
                cpu.W.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 2 -> {
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 3 -> {
                cpu.controlUnit.sendWriteSignal();
                cpu.PC.getHigh().emit();
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 4 -> {
                cpu.PC.getLow().emit();
                cpu.SP.emit();
                cpu.idu.copyFormSoC();
                cpu.SP.sampleFromIduBus();
                cpu.PC.set(cpu.WZ.get());
                return false;
            }
            case 5 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
        }
        throw  new IllegalStateException("Step non valido per Call nn: " + step);
    }
}
