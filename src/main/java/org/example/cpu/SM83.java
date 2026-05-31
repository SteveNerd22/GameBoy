package org.example.cpu;

import org.example.bus.*;
import org.example.cpu.pipeline.ExecutionEngine;

public class SM83 implements BusWriter {


    public final Register IR, IE;
    public final Register A, F, B, C, D, E, H, L;

    public final RegisterPair AF, BC, DE, HL;
    public final PointerRegister PC, SP;

    public final ControlUnit controlUnit;
    public final Alu alu;
    public final Idu idu;

    public final InterruptBus SoCInterrupts;
    public final DataBus SoCData;
    public final AddressBus SoCAddress;

    private final ExecutionEngine engine;

    SM83(InterruptBus SoCInterrupts, DataBus SoCData, AddressBus SoCAddress) {
        this.SoCInterrupts = SoCInterrupts;
        this.SoCData = SoCData;
        this.SoCAddress = SoCAddress;

        engine = new ExecutionEngine();
        controlUnit = new ControlUnit(SoCInterrupts, engine);

        AddressBus iduToAddressRegisters = new AddressBus();
        DataBus regToAluBus1 = new DataBus();
        DataBus regToAluBus2 = new DataBus();

        // --- 1. SPECIAL REGISTERS ---
        // IR (Instruction Register) only talks to the internal ALU buses to decode/execute logic
        this.IR = new Register(regToAluBus1, regToAluBus2);

        // IE (Interrupt Enable) sits on SoCData and can feed the ALU logic
        this.IE = new Register(SoCData, regToAluBus1, regToAluBus2);

        // --- 2. THE CHOSEN ONES (Direct ALU Access) ---
        // A (Accumulator) and F (Flags) are hardwired to everything data-related
        this.A = new Register(SoCData, regToAluBus1, regToAluBus2);
        this.F = new Register(SoCData, regToAluBus1, regToAluBus2);

        // B is often used as a primary operand for 8-bit math, so it hooks to the ALU buses
        this.B = new Register(SoCData, regToAluBus1, regToAluBus2);

        // --- 3. GENERAL PURPOSE REGISTERS (SoC Data Only) ---
        // These registers pass through the main data lines to move data around,
        // but they don't have a direct private highway into the ALU
        this.C = new Register(SoCData);
        this.D = new Register(SoCData);
        this.E = new Register(SoCData);
        this.H = new Register(SoCData);
        this.L = new Register(SoCData);

        // --- CREAZIONE DELLE COPPIE (Sopra i registri appena nati) ---
        this.AF = new RegisterPair(this.A, this.F, SoCAddress);
        this.BC = new RegisterPair(this.B, this.C, SoCAddress);
        this.DE = new RegisterPair(this.D, this.E, SoCAddress);
        this.HL = new RegisterPair(this.H, this.L, SoCAddress);

        // --- 4. POINTER REGISTERS (16-bit Architecture) ---
        // Dedicated layout for memory management and the internal IDU feedback loop
        this.PC = new PointerRegister(SoCData, SoCAddress, iduToAddressRegisters);
        this.SP = new PointerRegister(SoCData, SoCAddress, iduToAddressRegisters);

        alu = new Alu(SoCData);
        idu = new Idu(SoCAddress, iduToAddressRegisters);
        try { Class.forName("org.example.cpu.pipeline.instructions.InstructionRegistry"); } catch (Exception e) {}
    }

    /**
     * Il vero e unico punto di ingresso del Clock della CPU.
     * Il SoC chiama questo metodo a ogni ciclo di clock (T-Cycle).
     */
    public void pulse() {
        controlUnit.pulse(this);
    }

    /**
     * Metodo di diagnostica per i test per sapere in che stato si trova la pipeline.
     */
    public String getPipelineStatus() {
        return this.engine.getCurrentStage().toString();
    }
}
