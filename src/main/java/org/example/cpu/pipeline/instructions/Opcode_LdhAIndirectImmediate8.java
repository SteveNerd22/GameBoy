package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.bus.data.AddressData;

@CpuOpcode(value = {0xF0}) // LDH A, (n)
public final class Opcode_LdhAIndirectImmediate8 extends CpuInstruction {

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
                cpu.Z.emitToInternalData();

                int combinedAddress = (0xFF << 8) | (cpu.Z.sampleInternalData() & 0xFF);
                cpu.SoCAddress.broadcast(cpu.Z, new AddressData(combinedAddress));

                cpu.Z.sampleSoCBus();

                cpu.Z.emitToInternalData();
                cpu.A.sampleInternalData();

                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LDH A, (n): " + step);
        }
    }
}