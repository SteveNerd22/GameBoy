package org.example.cpu;

import org.example.bus.BusReader;
import org.example.bus.BusWriter;
import org.example.bus.InterruptBus;
import org.example.bus.data.InterruptSignal;
import org.example.cpu.pipeline.ExecutionEngine;

import java.lang.reflect.Executable;

public class ControlUnit implements BusWriter, BusReader<InterruptSignal> {

    InterruptBus SoCInterrupts;

    private int internalCycleCounter = 0;
    private boolean isHalting = false;

    private ExecutionEngine executionEngine;

    public ControlUnit(InterruptBus SoCInterrupts, ExecutionEngine engine) {
        this.SoCInterrupts = SoCInterrupts;
        this.executionEngine = engine;
        SoCInterrupts.registerWriter(this);
        SoCInterrupts.registerReader(this);
    }

    public void pulse(SM83 sm83) {
        executionEngine.pulse(sm83);
    }



    private void handleInterruptRoutine(SM83 cpu, int interruptMask) {
        // Gestione hardware dei vettori di salto degli interrupt
    }

    @Override
    public void onBusWrite(BusWriter sender, InterruptSignal data) {
        // Reazione immediata ad eventi asincroni sul bus degli interrupt (se necessaria)
    }

    public void reset() {
        internalCycleCounter = 0;
        isHalting = false;
        executionEngine.reset();
    }
}