package org.example.bus;

import org.example.bus.data.InterruptSignal;

public class InterruptBus extends Bus<InterruptSignal> {

    @Override
    protected void send(BusWriter sender, InterruptSignal data, BusReader<InterruptSignal> reader) {
        reader.onBusWrite(sender, data);
    }

    @Override
    public void onBusWrite(BusWriter sender, InterruptSignal data) {
        broadcast(sender, data);
    }

    public int sampleSignal() {
        return sample().getBitMask();
    }
}