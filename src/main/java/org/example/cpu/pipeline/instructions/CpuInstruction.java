package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

public abstract sealed class CpuInstruction permits Opcode_LdAbsolute, Opcode_LdHighRam, Opcode_LdHlIncDec, Opcode_LdRegHl, Opcode_LdRegImmediate, Opcode_LdRegIndirect, Opcode_LdRegReg, Opcode_Nop, UnimplementedInstruction
{

    protected int currentStep = 0;
    protected int currentOpcode = 0;
    protected boolean finished = false;

    /**
     * Inizializza lo stato dell'istruzione prima dell'esecuzione.
     */
    public final void prepare() {
        this.currentStep = 0;
        this.finished = false;
    }



    public void setTriggeredOpcode(int currentOpcode) {
        this.currentOpcode = currentOpcode;
    }

    /**
     * Il metodo che le singole istruzioni dovranno implementare per definire
     * cosa fare a ogni ciclo macchina (M-Cycle).
     */
    public abstract void executeCycle(SM83 cpu);

    /**
     * Interrogato dall'Enum per capire se l'istruzione è conclusa.
     */
    public final boolean isFinished() {
        return this.finished;
    }

    // ==========================================
    //  METODI AUSILIARI STRUTTURALI (API FRAMEWORK)
    // ==========================================

    /**
     * Forza il Program Counter a pilotare l'Address Bus esterno.
     */
    protected final void emitProgramCounter(SM83 cpu) {
        cpu.PC.emitAddress();
    }

    /**
     * Avanza il Program Counter di una posizione sfruttando il ciclo privato dell'IDU.
     */
    protected final void advanceProgramCounter(SM83 cpu) {
        cpu.idu.increment(cpu.PC.get());
    }

    /**
     * Campiona l'attuale byte fluttuante sul Data Bus del SoC.
     */
    protected final int sampleDataBus(SM83 cpu) {
        return cpu.SoCData.sampleByte();
    }

    /**
     * Segnala esplicitamente che l'istruzione ha completato tutte le sue fasi hardware.
     */
    protected final void terminate() {
        this.finished = true;
    }

    Register resolveDestRegister(int opcode, SM83 cpu) {
        // Spostiamo a destra di 3 bit e isoliamo gli ultimi 3 (maschera 0x07, cioè binario 111)
        int regIndex = (opcode >> 3) & 0x07;
        return mapIndexToRegister(regIndex, cpu);
    }

    Register resolveSourceRegister(int opcode, SM83 cpu) {
        // Isoliamo direttamente gli ultimi 3 bit (maschera 0x07)
        int regIndex = opcode & 0x07;
        return mapIndexToRegister(regIndex, cpu);
    }

    private Register mapIndexToRegister(int index, SM83 cpu) {
        return switch (index) {
            case 0 -> cpu.B;
            case 1 -> cpu.C;
            case 2 -> cpu.D;
            case 3 -> cpu.E;
            case 4 -> cpu.H;
            case 5 -> cpu.L;
            case 7 -> cpu.A;
            default -> throw new IllegalArgumentException("Indice registro invalido: " + index);
        };
    }
}