package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.bus.data.AddressData;

@CpuOpcode(value = {0xE2}) // LDH (C), A
public final class Opcode_LdhIndirectCA extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.C.emitToInternalData();

            int combinedAddress = (0xFF << 8) | (cpu.C.sampleInternalData() & 0xFF);
            cpu.SoCAddress.broadcast(cpu.C, new AddressData(combinedAddress));

            cpu.A.emit();

            return true;
        }

        throw new IllegalStateException("Step non valido per LDH (C), A: " + step);
    }
}