package org.example.cpu.pipeline.instructions;

import org.example.Main;
import org.example.cpu.SM83;

public final class UnimplementedInstruction extends CpuInstruction {
    private final int opcode;
    private final boolean isCb;

    public UnimplementedInstruction(int opcode, boolean isCb) {
        this.opcode = opcode;
        this.isCb = isCb;
    }

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        String prefix = isCb ? "0xCB " : "";
        if(Main.DEBUG)
            return true;
        System.err.printf("FATAL: Opcode %s0x%02X is not implemented yet! PC: 0x%04X\n",
                prefix, opcode, cpu.PC.get());
        System.exit(1);
        return false;
    }
}