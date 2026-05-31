package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.SM83;

@CpuOpcode(value = {0xC5, 0xD5, 0xE5, 0xF5})
public final class Opcode_Push extends CpuInstruction {

    @Override
    public void executeCycle(SM83 cpu) {
        // Risolviamo il reference al registro a 16-bit che dobbiamo pushare
        Contextual16BitRef regSource = resolveStackRegister16(this.currentOpcode, cpu);

        switch (this.currentStep) {
            case 0 -> {
                // Lo stack cresce all'indietro: decrementiamo lo Stack Pointer
                cpu.SP.setValue(cpu.SP.get() - 1);

                // 1. Lo Stack Pointer guida l'Address Bus
                cpu.SP.emitAddress();

                // 2. HARDWARE PURO: Il mezzo registro ALTO (es. B) pulsa direttamente sul Data Bus!
                regSource.highReg().emit();

                this.currentStep = 1;
            }
            case 1 -> {
                // Decrementiamo SP per il secondo byte
                cpu.SP.setValue(cpu.SP.get() - 1);

                // 1. Lo Stack Pointer guida l'Address Bus sul nuovo indirizzo
                cpu.SP.emitAddress();

                // 2. HARDWARE PURO: Il mezzo registro BASSO (es. C) pulsa direttamente sul Data Bus!
                regSource.lowReg().emit();

                this.currentStep = 2;
            }
            case 2 -> {
                // Ciclo di stabilità hardware finale per la scrittura della RAM
                terminate();
            }
        }
    }
}