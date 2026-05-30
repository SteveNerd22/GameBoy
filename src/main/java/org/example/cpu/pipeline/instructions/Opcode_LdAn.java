package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0x3E)
public final class Opcode_LdAn extends CpuInstruction {

    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0:
                emitProgramCounter(cpu);
                advanceProgramCounter(cpu);

                this.currentStep = 1;
                break;

            case 1:
                int immediateValue = sampleDataBus(cpu);
                cpu.A.setValue(immediateValue);

                terminate();
                break;
        }
    }
}