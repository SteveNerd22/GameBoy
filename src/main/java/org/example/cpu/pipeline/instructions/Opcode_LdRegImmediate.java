package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x06, 0x0E, 0x16, 0x1E, 0x26, 0x2E, 0x3E})
public final class Opcode_LdRegImmediate extends CpuInstruction {

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

                Register targetRegister = resolveDestRegister(this.currentOpcode, cpu);
                targetRegister.setValue(immediateValue);

                terminate();
                break;
        }
    }
}