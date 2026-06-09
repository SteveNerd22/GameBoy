package org.example.cpu;

public interface DebugCommand {
    /**
     * @return Il nome del comando (es. "reg", "vram")
     */
    String getKeyword();

    /**
     * Esegue la logica del comando.
     * @return true se il comando fa avanzare il tempo (step), false se è solo ispettivo.
     */
    boolean execute(DebugContext ctx, String[] args);

    /**
     * @return Una riga di aiuto per il comando
     */
    String getHelp();
}