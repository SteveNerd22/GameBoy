package org.example.bus;

import org.example.bus.data.ByteData;

public class DataBus extends Bus<ByteData> {

    @Override
    protected void send(BusWriter sender, ByteData data, BusReader<ByteData> reader) {
        reader.onBusWrite(sender, data);
    }

    @Override
    public void onBusWrite(BusWriter sender, ByteData data) {
        broadcast(sender, data);
    }

    public int sampleByte() {
        return (sample() != null) ? sample().getByteValue() : 0xFF;
    }
}