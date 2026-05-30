package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

public final class UnimplementedInstruction extends CpuInstruction {
    private final int opcode;
    private final boolean isCb;

    public UnimplementedInstruction(int opcode, boolean isCb) {
        this.opcode = opcode;
        this.isCb = isCb;
    }

    @Override
    public void executeCycle(SM83 cpu) {
        String prefix = isCb ? "0xCB " : "";
        System.err.printf("FATAL: Opcode %s0x%02X is not implemented yet! PC: 0x%04X\n",
                prefix, opcode, cpu.PC.get());
        System.exit(1);
    }
}