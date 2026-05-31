package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0xF9)
public final class Opcode_LdSpHl extends CpuInstruction {
    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0 -> {
                // M-CYCLE 1: Trasferimento interno dei 16-bit
                cpu.SP.setValue(cpu.HL.get());
                this.currentStep = 1;
            }
            case 1 -> {
                // M-CYCLE 2: Ciclo di ritardo interno per stabilizzazione circuitale
                terminate();
            }
        }
    }
}