package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

public abstract sealed class CpuInstruction permits Opcode_LdAHl, Opcode_LdAn, Opcode_LdBa, Opcode_Nop, UnimplementedInstruction
{

    protected int currentStep = 0;
    protected boolean finished = false;

    /**
     * Inizializza lo stato dell'istruzione prima dell'esecuzione.
     */
    public final void prepare() {
        this.currentStep = 0;
        this.finished = false;
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
}