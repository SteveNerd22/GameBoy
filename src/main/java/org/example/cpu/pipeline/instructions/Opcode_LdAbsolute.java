package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.SM83;

@CpuOpcode(value = {0xEA, 0xFA})
public final class Opcode_LdAbsolute extends CpuInstruction {
    private int lowByte = 0;
    private int address = 0;

    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0 -> {
                // M-CYCLE 1: Fetch del byte basso dell'indirizzo immediato 'nn'
                emitProgramCounter(cpu);
                advanceProgramCounter(cpu);
                this.currentStep = 1;
            }
            case 1 -> {
                // M-CYCLE 2: Leggiamo il byte basso e facciamo il fetch del byte alto
                this.lowByte = sampleDataBus(cpu);
                emitProgramCounter(cpu);
                advanceProgramCounter(cpu);
                this.currentStep = 2;
            }
            case 2 -> {
                // M-CYCLE 3: Leggiamo il byte alto e ricomponiamo l'indirizzo finale a 16-bit
                int highByte = sampleDataBus(cpu);
                this.address = (highByte << 8) | this.lowByte;

                // Piazziamo l'indirizzo sul bus. Chi pilota? La CPU stessa tramite la sua ALU/Internal Buffer
                cpu.SoCAddress.broadcast(cpu, new AddressData(this.address));

                // Se è una scrittura (0xEA), il registro A inizia a pilotare il Data Bus
                if (this.currentOpcode == 0xEA) {
                    cpu.A.emit();
                }
                this.currentStep = 3;
            }
            case 3 -> {
                // M-CYCLE 4: Ciclo di stabilità hardware finale
                // I bus sono ancora occupati da quello che abbiamo configurato nel case 2

                if (this.currentOpcode == 0xFA) {
                    // Se è una lettura, la MMU ha piazzato il dato sul bus: lo salviamo in A
                    cpu.A.setValue(sampleDataBus(cpu));
                } else {
                    // Se è una scrittura, questo ciclo serve alla memoria/RAM per completare
                    // l'immagazzinamento del dato emesso da A. Lasciamo i bus stabili.
                }

                // L'istruzione ha esaurito i suoi 4 M-cycles, possiamo chiudere.
                terminate();
            }
        }
    }
}