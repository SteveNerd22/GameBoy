package org.example.cpu;

public interface EmulationEndingHandler {
    /**
     * Chiamato quando l'emulazione termina normalmente (es. comando 'stop' o fine naturale).
     */
    void onExit(DebugContext ctx);

    /**
     * Chiamato se l'emulazione si interrompe bruscamente a causa di un'eccezione imprevista.
     */
    void onError(DebugContext ctx, Throwable error);
}