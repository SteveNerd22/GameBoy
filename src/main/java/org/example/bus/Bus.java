package org.example.bus;

import org.example.bus.data.BusData;

import java.util.ArrayList;
import java.util.List;

public abstract class Bus<T extends BusData> implements BusReader<T> {
    protected final List<BusReader<T>> readers = new ArrayList<>();
    protected final List<BusWriter> writers = new ArrayList<>();
    protected T data;

    public void registerReader(BusReader<T> reader) {
        this.readers.add(reader);
    }

    public void registerWriter(BusWriter writer) {
        this.writers.add(writer);
    }

    /**
     * Universal broadcast. It just pushes the abstract BusData to everyone.
     */
    public void broadcast(BusWriter sender, T data) {
        this.data = data;
        for (BusReader<T> reader : readers) {
            if (reader != sender) {
                send(sender, data, reader);
            }
        }
    }

    protected T sample() {
        return data;
    }

    protected abstract void send(BusWriter sender, T data, BusReader<T> reader);
}