package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.SM83;
import org.example.cpu.pipeline.instructions.CpuInstruction;

@CpuOpcode(value = 0x7E)
public final class Opcode_LdAHl extends CpuInstruction {

    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0:
                int targetAddress = cpu.HL.get();
                cpu.SoCAddress.broadcast(cpu.H, new AddressData(targetAddress));

                this.currentStep = 1;
                break;

            case 1:
                int memoryValue = sampleDataBus(cpu);
                cpu.A.setValue(memoryValue);

                terminate();
                break;
        }
    }
}