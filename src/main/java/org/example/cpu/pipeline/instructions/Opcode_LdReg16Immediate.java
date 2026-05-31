package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x01, 0x11, 0x21, 0x31})
public final class Opcode_LdReg16Immediate extends CpuInstruction {
    private int internalLatchLow = 0;

    @Override
    public void executeCycle(SM83 cpu) {
        Contextual16BitRef targetReg = resolveRegister16(this.currentOpcode, cpu);

        switch (this.currentStep) {
            case 0 -> {
                emitProgramCounter(cpu);
                advanceProgramCounter(cpu);
                this.currentStep = 1;
            }
            case 1 -> {
                int sampledLow = sampleDataBus(cpu);

                if (targetReg.lowReg() != null) {
                    targetReg.lowReg().setValue(sampledLow);
                } else {
                    this.internalLatchLow = sampledLow;
                }

                emitProgramCounter(cpu);
                advanceProgramCounter(cpu);
                this.currentStep = 2;
            }
            case 2 -> {
                int sampledHigh = sampleDataBus(cpu);

                if (targetReg.highReg() != null) {
                    targetReg.highReg().setValue(sampledHigh);
                } else {
                    int fullValue = (sampledHigh << 8) | this.internalLatchLow;
                    targetReg.set(fullValue);
                }

                terminate();
            }
        }
    }
}