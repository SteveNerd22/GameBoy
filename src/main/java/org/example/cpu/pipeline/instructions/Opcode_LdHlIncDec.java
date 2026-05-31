package org.example.cpu.pipeline.instructions;

import org.example.bus.data.ByteData;
import org.example.cpu.SM83;
import org.example.cpu.pipeline.instructions.CpuInstruction;

@CpuOpcode(value = {0x22, 0x2A, 0x32, 0x3A})
public final class Opcode_LdHlIncDec extends CpuInstruction {
    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0 -> {
                cpu.HL.emit();

                if (this.currentOpcode == 0x22 || this.currentOpcode == 0x32) {
                    cpu.A.emit();
                }
                this.currentStep = 1;
            }
            case 1 -> {
                if (this.currentOpcode == 0x2A || this.currentOpcode == 0x3A) {
                    cpu.A.setValue(sampleDataBus(cpu));
                }

                if (this.currentOpcode == 0x22 || this.currentOpcode == 0x2A) {
                    cpu.HL.set(cpu.HL.get() + 1); // LI (Increment)
                } else {
                    cpu.HL.set(cpu.HL.get() - 1); // LD (Decrement)
                }
                terminate();
            }
        }
    }
}