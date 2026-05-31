package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.SM83;

@CpuOpcode(value = 0x08)
public final class Opcode_LdAbsoluteSP extends CpuInstruction {
    private int lowByteAddr = 0;
    private int targetAddress = 0;

    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0 -> {
                emitProgramCounter(cpu);
                advanceProgramCounter(cpu);
                this.currentStep = 1;
            }
            case 1 -> {
                this.lowByteAddr = sampleDataBus(cpu);
                emitProgramCounter(cpu);
                advanceProgramCounter(cpu);
                this.currentStep = 2;
            }
            case 2 -> {
                // 1. Leggiamo l'ultimo byte dell'indirizzo e lo ricomponiamo
                int highByteAddr = sampleDataBus(cpu);
                this.targetAddress = (highByteAddr << 8) | this.lowByteAddr;

                // 2. La CPU guida l'Address Bus con l'indirizzo base 'nn' (es. $C000)
                cpu.SoCAddress.broadcast(cpu, new org.example.bus.data.AddressData(this.targetAddress));

                // 3. La CPU estrae i 8 bit bassi di SP e li isola sul Data Bus
                int lowSpByte = cpu.SP.get() & 0xFF;
                cpu.SoCData.broadcast(cpu, new org.example.bus.data.ByteData(lowSpByte));

                this.currentStep = 3;
            }
            case 3 -> {
                // Al ciclo successivo, la RAM ha salvato il byte basso a 'nn'.
                // Ora la CPU deve scrivere il byte alto a 'nn + 1' (es. $C001).

                // 1. Cambiamo l'indirizzo sul bus incrementandolo di 1
                cpu.SoCAddress.broadcast(cpu, new org.example.bus.data.AddressData(this.targetAddress + 1));

                // 2. La CPU estrae i 8 bit alti di SP e li isola sul Data Bus
                int highSpByte = (cpu.SP.get() >> 8) & 0xFF;
                cpu.SoCData.broadcast(cpu, new org.example.bus.data.ByteData(highSpByte));

                this.currentStep = 4;
            }
            case 4 -> {
                // M-CYCLE 5: Ciclo di stabilità finale per dare tempo alla RAM
                // di assimilare il secondo byte prima di chiudere i rubinetti.
                terminate();
            }
        }
    }
}