package org.example.cpu;

import org.example.bus.BusWriter;

public interface IRegister extends BusWriter {
    /**
     * Legge il valore corrente del registro.
     */
    int get();

    /**
     * Scrive un nuovo valore nel registro.
     */
    void set(int value);

    /**
     * Attiva i pass-gate per presentare il valore sul rispettivo Bus.
     */
    void emit();
}