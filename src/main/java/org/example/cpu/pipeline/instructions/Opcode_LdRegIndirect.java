package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x02, 0x0A, 0x12, 0x1A})
public final class Opcode_LdRegIndirect extends CpuInstruction {
    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0 -> {
                boolean opcode = this.currentOpcode == 0x02 || this.currentOpcode == 0x0A;
                if (opcode) {
                    cpu.BC.emit();
                } else {
                    cpu.DE.emit();
                }

                if (opcode) {
                    cpu.A.emit();
                }

                this.currentStep = 1;
            }
            case 1 -> {
                if (this.currentOpcode == 0x0A || this.currentOpcode == 0x1A) {
                    cpu.A.setValue(sampleDataBus(cpu));
                }

                terminate();
            }
        }
    }
}