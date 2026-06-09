package org.example.cpu;

import org.example.bus.BusReader;
import org.example.bus.BusWriter;
import org.example.bus.InterruptBus;
import org.example.bus.data.InterruptSignal;
import org.example.cpu.pipeline.ExecutionEngine;

public class ControlUnit implements BusWriter, BusReader<InterruptSignal> {

    private final InterruptBus SoCInterrupts;
    private final ExecutionEngine executionEngine;
    private boolean isHalting = false, isStopping = false;
    private boolean isCBPrefix = false;

    // PRE-ALLOCAZIONE DEI SEGNALI: Evita di fare "new" a ogni M-Cycle sul bus
    private final InterruptSignal readSignal = new InterruptSignal(InterruptSignal.MEM_RD);
    private final InterruptSignal writeSignal = new InterruptSignal(InterruptSignal.MEM_WR);
    private final InterruptSignal clearSignal = new InterruptSignal(InterruptSignal.NONE);

    public ControlUnit(InterruptBus SoCInterrupts, ExecutionEngine engine) {
        this.SoCInterrupts = SoCInterrupts;
        this.executionEngine = engine;

        this.SoCInterrupts.registerWriter(this);
        this.SoCInterrupts.registerReader(this);
    }

    /**
     * Spara l'impulso elettrico di LETTURA sull'InterruptBus.
     * Dice alla MMU di prendere l'indirizzo corrente dall'AddressBus e buttare il dato sul DataBus.
     */
    public void sendReadSignal() {
        this.SoCInterrupts.broadcast(this, this.readSignal);
    }

    /**
     * Spara l'impulso elettrico di SCRITTURA sull'InterruptBus.
     * Dice alla MMU di prendere il dato fluttuante sul DataBus e scriverlo all'indirizzo dell'AddressBus.
     */
    public void sendWriteSignal() {
        this.SoCInterrupts.broadcast(this, this.writeSignal);
    }

    /**
     * Opzionale: Pulisce la linea di controllo (rilascia i segnali di RD/WR sul bus).
     */
    public void clearControlSignals() {
        this.SoCInterrupts.broadcast(this, this.clearSignal);
    }

    public void pulse(SM83 sm83) {
        // Il pulse passa la CPU all'ExecutionEngine per eseguire lo step dell'opcode corrente
        executionEngine.pulse(sm83, this.isCBPrefix);
    }

    private void handleInterruptRoutine(SM83 cpu, int interruptMask) {
        // Gestione hardware dei vettori di salto degli interrupt
    }

    @Override
    public void onBusWrite(BusWriter sender, InterruptSignal data) {
        // Reazione immediata ad eventi asincroni sul bus degli interrupt (se necessaria)
    }

    public void reset() {
        isHalting = false;
        isStopping = false;
        isCBPrefix = false;
        sendReadSignal();
        executionEngine.reset();
    }

    public long getTotalTicks() {
        return executionEngine.getTotalTicks();
    }

    public void enableInterrupt() {
        // TODO: aggiungere supporto per interrupt
    }

    public void disableInterrupt() {
        // TODO: aggiungere supporto per interrupt
    }

    public void enterHaltMode() {
        this.isHalting = true;
    }

    public void enterStopMode() {
        this.isStopping = true;
    }

    public void resetCBPrefix() {
        isCBPrefix = false;
    }

    public void setCBPrefix() {
        this.isCBPrefix = true;
    }

}