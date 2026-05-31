package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xC1, 0xD1, 0xE1, 0xF1})
public final class Opcode_Pop extends CpuInstruction {

    @Override
    public void executeCycle(SM83 cpu) {
        Contextual16BitRef targetReg = resolveStackRegister16(this.currentOpcode, cpu);

        switch (this.currentStep) {
            case 0 -> {
                cpu.SP.emitAddress();
                this.currentStep = 1;
            }
            case 1 -> {
                int sampledLow = sampleDataBus(cpu);

                if (this.currentOpcode == 0xF1) {
                    targetReg.lowReg().setValue(sampledLow & 0xF0);
                } else {
                    targetReg.lowReg().setValue(sampledLow);
                }

                cpu.SP.setValue(cpu.SP.get() + 1);

                cpu.SP.emitAddress();
                this.currentStep = 2;
            }
            case 2 -> {
                int sampledHigh = sampleDataBus(cpu);
                targetReg.highReg().setValue(sampledHigh);
                
                cpu.SP.setValue(cpu.SP.get() + 1);

                terminate();
            }
        }
    }
}