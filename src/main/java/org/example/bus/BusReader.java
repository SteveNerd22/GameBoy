package org.example.bus;

import org.example.bus.data.BusData;

public interface BusReader<T extends BusData> {
    void onBusWrite(BusWriter sender, T data);
}