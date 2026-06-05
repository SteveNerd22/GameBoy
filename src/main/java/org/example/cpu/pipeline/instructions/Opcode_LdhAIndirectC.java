package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.SM83;

@CpuOpcode(value = {0xF2}) // LDH A, (C)
public final class Opcode_LdhAIndirectC extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.C.emitToInternalData();
                int lowByte = cpu.C.sampleInternalData() & 0xFF;
                int highByte = 0xFF;
                int combinedAddress = (highByte << 8) | lowByte;
                cpu.SoCAddress.broadcast(cpu.C, new AddressData(combinedAddress));
                cpu.Z.sampleSoCBus();
                return false;
            }
            case 1 -> {
                cpu.PC.emit();
                cpu.Z.emitToInternalData();
                cpu.A.sampleInternalData();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per LDH A, (C): " + step);
    }
}