package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

public abstract sealed class CpuInstruction permits Opcode_AddIndirectHl, Opcode_AddRegister8, Opcode_LdAIndirectBcDe, Opcode_LdAIndirectHlIncDec, Opcode_LdAIndirectImmediate16, Opcode_LdHlSpImmediateSign8, Opcode_LdIndirectBcDeA, Opcode_LdIndirectHlImmediate, Opcode_LdIndirectHlIncDecA, Opcode_LdIndirectHlReg, Opcode_LdIndirectImmediate16A, Opcode_LdIndirectImmediate16Sp, Opcode_LdRegImmediate, Opcode_LdRegIndirectHl, Opcode_LdRegReg, Opcode_LdRegisterPairImmediate16, Opcode_LdSpHl, Opcode_LdhAIndirectC, Opcode_LdhAIndirectImmediate8, Opcode_LdhIndirectCA, Opcode_LdhIndirectImmediate8A, Opcode_Nop, Opcode_Pop, Opcode_Push, UnimplementedInstruction {

    private int currentStep = 0;
    private boolean finished = false;
    int currentOpcode = 0;

    public final void prepare(int opcode) {
        this.currentOpcode = opcode;
        this.currentStep = 0;
        this.finished = false;
    }

    public final void execute(SM83 cpu) {
        if (this.finished) return;

        boolean isOpcodeExecutionDone = executeStep(this.currentStep, this.currentOpcode, cpu);

        if (isOpcodeExecutionDone) {
            this.finished = true;
        } else {
            this.currentStep++;
        }
    }

    protected abstract boolean executeStep(int step, int opcode, SM83 cpu);

    public final boolean isFinished() {
        return this.finished;
    }

    // =========================================================================
    //          GESTIONE HARDWARE PROGRAM COUNTER (IDU FEEDBACK LOOP)
    // =========================================================================

    /**
     * Incrementa il PC completando il ciclo: l'IDU elabora e il PC campiona dal bus privato.
     */
    protected final void advanceProgramCounter(SM83 cpu) {
        cpu.idu.increment(cpu.PC.get());
        cpu.PC.sampleFromIduBus();
    }

    /**
     * Decrementa il PC completando il ciclo tramite l'IDU.
     */
    protected final void decrementProgramCounter(SM83 cpu) {
        cpu.idu.decrement(cpu.PC.get());
        cpu.PC.sampleFromIduBus();
    }

    protected final int sampleDataBus(SM83 cpu) {
        return cpu.SoCData.sampleByte() & 0xFF;
    }

    // --- INDICIZZAZIONE REGISTRI A 8-BIT ---

    protected final Register resolveSourceRegister(int opcode, SM83 cpu) {
        return mapIndexToRegister(opcode & 0x07, cpu);
    }

    protected final Register resolveDestRegister(int opcode, SM83 cpu) {
        return mapIndexToRegister((opcode >> 3) & 0x07, cpu);
    }

    private Register mapIndexToRegister(int index, SM83 cpu) {
        return switch (index) {
            case 0 -> cpu.B;
            case 1 -> cpu.C;
            case 2 -> cpu.D;
            case 3 -> cpu.E;
            case 4 -> cpu.H;
            case 5 -> cpu.L;
            // Il caso 6 è (HL), gestito dalle istruzioni di memoria
            case 7 -> cpu.A;
            default -> throw new IllegalArgumentException("Indice registro 8-bit invalido: " + index);
        };
    }

    // --- INDICIZZAZIONE COPPIE DI REGISTRI A 16-BIT ---

    /**
     * Mappatura standard per istruzioni a 16-bit (es. LD dd, nn o ADD HL, ss).
     * Bit 4-5 dell'opcode determinano la coppia.
     */
    protected final RegisterPair resolveRegisterPair(int opcode, SM83 cpu) {
        int index = (opcode >> 4) & 0x03;
        return switch (index) {
            case 0 -> cpu.BC;
            case 1 -> cpu.DE;
            case 2 -> cpu.HL;
            case 3 -> cpu.SP;
            default -> throw new IllegalArgumentException("Indice coppia 16-bit invalido: " + index);
        };
    }

    /**
     * Mappatura specifica per le istruzioni PUSH/POP (dove l'indice 3 è AF invece di SP).
     */
    protected final RegisterPair resolveRegisterPairForStack(int opcode, SM83 cpu) {
        int index = (opcode >> 4) & 0x03;
        return switch (index) {
            case 0 -> cpu.BC;
            case 1 -> cpu.DE;
            case 2 -> cpu.HL;
            case 3 -> cpu.AF;
            default -> throw new IllegalArgumentException("Indice coppia Stack invalido: " + index);
        };
    }
}