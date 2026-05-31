package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.SM83;

@CpuOpcode(value = {0xE0, 0xF0, 0xE2, 0xF2})
public final class Opcode_LdHighRam extends CpuInstruction {

    // Stato interno per conservare l'offset tra un ciclo di clock (M-Cycle) e l'altro
    private int targetOffset = 0;

    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {

            case 0 -> {
                if (this.currentOpcode == 0xE0 || this.currentOpcode == 0xF0) {
                    emitProgramCounter(cpu);
                    advanceProgramCounter(cpu);

                    this.currentStep = 1;
                } else {
                    this.targetOffset = cpu.C.get();

                    int finalAddress = 0xFF00 + this.targetOffset;
                    cpu.SoCAddress.broadcast(cpu, new AddressData(finalAddress));

                    if (this.currentOpcode == 0xE2) {
                        cpu.A.emit();
                    }

                    this.currentStep = 1;
                }
            }

            case 1 -> {
                if (this.currentOpcode == 0xE0 || this.currentOpcode == 0xF0) {
                    this.targetOffset = sampleDataBus(cpu);

                    int finalAddress = 0xFF00 + this.targetOffset;
                    cpu.SoCAddress.broadcast(cpu, new AddressData(finalAddress));

                    if (this.currentOpcode == 0xE0) {
                        cpu.A.emit();
                    }

                    this.currentStep = 2;
                } else {
                    if (this.currentOpcode == 0xF2) {
                        cpu.A.setValue(sampleDataBus(cpu));
                    }
                    terminate();
                }
            }

            case 2 -> {
                if (this.currentOpcode == 0xF0) {
                    cpu.A.setValue(sampleDataBus(cpu));
                }
                terminate();
            }
        }
    }
}