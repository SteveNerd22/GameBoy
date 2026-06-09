package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0xC7, // RST 00h
        0xCF, // RST 08h
        0xD7, // RST 10h
        0xDF, // RST 18h
        0xE7, // RST 20h
        0xEF, // RST 28h
        0xF7, // RST 30h
        0xFF  // RST 38h
})
public final class Opcode_RestartAbsolute extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 1 -> {
                cpu.controlUnit.sendWriteSignal();
                cpu.PC.getHigh().emit();
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 2 -> {
                cpu.PC.getLow().emit();
                cpu.SP.emit();
                cpu.idu.copyFormSoC();
                cpu.SP.sampleFromIduBus();
                int vector = (opcode >> 3) & 0x07;
                int targetAddress = vector * 8;
                cpu.PC.set(targetAddress);
                return false;
            }
            case 3 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per RST: " + step);
    }
}