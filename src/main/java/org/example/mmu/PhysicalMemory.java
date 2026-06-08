package org.example.mmu;

import org.example.bus.BusWriter;

public class PhysicalMemory {
    private final int[] data;
    private BusWriter owner = null;

    public PhysicalMemory(int size) {
        this.data = new int[size];
    }

    /**
     * Tenta di leggere dalla memoria.
     * @param requestor Il componente che richiede la lettura (es. MMU, PPU)
     * @param offset L'indirizzo relativo all'interno di questa memoria
     * @return Il valore letto, o 0xFF se l'accesso è negato (Bus Conflict)
     */
    public int read(BusWriter requestor, int offset) {
        if (owner != null && owner != requestor) {
            return 0xFF; // Conflitto sul bus: un altro componente ha il controllo esclusivo
        }
        return data[offset];
    }

    /**
     * Tenta di scrivere nella memoria.
     * @param requestor Il componente che richiede la scrittura
     */
    public void write(BusWriter requestor, int offset, int value) {
        if (owner == null || owner == requestor) {
            data[offset] = value & 0xFF;
        }
        // Se l'owner è qualcun altro, la scrittura della CPU viene ignorata (comportamento standard GB)
    }

    /**
     * Permette a un componente (es. la PPU) di bloccare il bus di questa memoria.
     */
    public void lock(BusWriter component) {
        this.owner = component;
    }

    /**
     * Rilascia il bus, rendendolo nuovamente accessibile a tutti.
     */
    public void unlock(BusWriter owner) {
        if (owner == this.owner)
            this.owner = null;
    }

    public int getSize() {
        return data.length;
    }
}