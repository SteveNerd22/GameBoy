package org.example.cpu;

import org.example.bus.*;
import org.example.cpu.pipeline.ExecutionEngine;

public class SM83 implements BusWriter {

    private long totalTicks = 0;

    public final Register IR, IE;
    public final Register A, B, C, D, E, H, L;
    public final FlagsRegister F;

    public final RegisterPair AF, BC, DE, HL;
    public final RegisterPair PC, SP;

    public final Register W;
    public final Register Z;
    public final RegisterPair WZ;

    public final ControlUnit controlUnit;
    public final Alu alu;
    public final Idu idu;

    public final InterruptBus SoCInterrupts;
    public final DataBus SoCData;
    public final AddressBus SoCAddress;

    private final ExecutionEngine engine;

    public SM83(InterruptBus SoCInterrupts, DataBus SoCData, AddressBus SoCAddress) {
        this.SoCInterrupts = SoCInterrupts;
        this.SoCData = SoCData;
        this.SoCAddress = SoCAddress;

        engine = new ExecutionEngine();
        controlUnit = new ControlUnit(SoCInterrupts, engine);

        // Linee di bus interne private del Core CPU
        AddressBus iduToAddressRegisters = new AddressBus();
        DataBus regToAluBus1 = new DataBus();
        DataBus regToAluBus2 = new DataBus();
        DataBus regToRegBus =  new DataBus();

        IR = new Register(SoCData, null, null, regToRegBus);
        IE = new Register(SoCData, null, null, regToRegBus);

        A = new Register(SoCData, regToAluBus1, regToAluBus2, regToRegBus);
        F = new FlagsRegister(SoCData, null, regToAluBus2, regToRegBus);
        AF = new RegisterPair(A, F, SoCAddress, iduToAddressRegisters);

        BC = new RegisterPair(SoCData, SoCAddress, iduToAddressRegisters, null, regToAluBus2, regToRegBus, regToRegBus);
        B = BC.getHigh();
        C = BC.getLow();

        DE = new RegisterPair(SoCData, SoCAddress, iduToAddressRegisters, null, regToAluBus2, regToRegBus, regToRegBus);
        D = DE.getHigh();
        E = DE.getLow();

        HL = new RegisterPair(SoCData, SoCAddress, iduToAddressRegisters, null, regToAluBus2, regToRegBus, regToRegBus);
        H = HL.getHigh();
        L = HL.getLow();

        PC = new RegisterPair(SoCData, SoCAddress, iduToAddressRegisters);
        SP = new RegisterPair(SoCData, SoCAddress, iduToAddressRegisters, null, regToAluBus2, regToRegBus, regToRegBus);

        W = new Register(SoCData, null, regToAluBus2, regToRegBus);
        Z = new Register(SoCData, null, regToAluBus2, regToRegBus);
        WZ = new RegisterPair(W, Z, SoCAddress, null);

        alu = new Alu(SoCData, regToAluBus1, regToAluBus2);
        idu = new Idu(SoCAddress, iduToAddressRegisters);
        try {
            Class.forName("org.example.cpu.pipeline.instructions.InstructionRegistry");
        } catch (Exception _) {}
    }

    /**
     * Il vero e unico punto di ingresso del Clock della CPU.
     * Il SoC chiama questo metodo a ogni ciclo di clock (T-Cycle).
     */
    public void pulse() {
        totalTicks++;
        controlUnit.pulse(this);
    }

    /**
     * Restituisce i T-Cycles totali accumulati dall'accensione della CPU.
     */
    public long getTotalTicks() {
        return this.totalTicks;
    }

    /**
     * Ripristina lo stato iniziale dei registri della CPU hardware.
     */
    public void reset() {
        A.set(0x01);
        F.set(0xB0);
        BC.set(0x0013);
        DE.set(0x00D8);
        HL.set(0x014D);
        SP.set(0xFFFE);
        PC.set(0x0000);

        IR.set(0x00);
        IE.set(0x00);
        W.set(0x00);
        Z.set(0x00);

        this.totalTicks = 0;

        controlUnit.reset();
    }

    /**
     * Metodo di diagnostica per i test per sapere in che stato si trova la pipeline.
     */
    public String getPipelineStatus() {
        return this.engine.getCurrentStage().toString();
    }
}
