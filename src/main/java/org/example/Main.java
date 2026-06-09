package org.example;

public class Main {
    // 🎭 MASCHERE DI BIT GLOBALI
    public static final int LOG_NONE       = 0;
    public static final int LOG_STATUS     = 1; // 1
    public static final int LOG_INIT       = 1 << 1; // 2
    public static final int LOG_BUS        = 1 << 2; // 4
    public static final int LOG_ALU        = 1 << 3; // 8
    public static final int LOG_REGISTERS  = 1 << 4; // 16
    public static final int LOG_INTERRUPT  = 1 << 5; // 32
    public static final int LOG_PPU        = 1 << 6; // 64
    public static final int LOG_ALL        = 0xFFFFFFFF;

    // 🎛️ FLAG GLOBALI MUTABILI (Status e Init attivi di default)
    public static boolean LOG_STATUS_ENABLED    = true;
    public static boolean LOG_INIT_ENABLED      = false;
    public static boolean LOG_BUS_ENABLED       = false;
    public static boolean LOG_ALU_ENABLED       = false;
    public static boolean LOG_REGISTERS_ENABLED = false;
    public static boolean LOG_INTERRUPT_ENABLED = false;
    public static boolean LOG_PPU_ENABLED       = false;

    /**
     * Il metodo che spacchetta l'intero con l'operazione OR e imposta i booleani statici.
     */
    public static void applyLogMask(int logMask) {
        LOG_STATUS_ENABLED    = (logMask & LOG_STATUS) != 0;
        LOG_INIT_ENABLED      = (logMask & LOG_INIT) != 0;
        LOG_BUS_ENABLED       = (logMask & LOG_BUS) != 0;
        LOG_ALU_ENABLED       = (logMask & LOG_ALU) != 0;
        LOG_REGISTERS_ENABLED = (logMask & LOG_REGISTERS) != 0;
        LOG_INTERRUPT_ENABLED = (logMask & LOG_INTERRUPT) != 0;
        LOG_PPU_ENABLED       = (logMask & LOG_PPU) != 0;
    }

    /**
     * Ricostruisce dinamicamente la maschera di bit corrente basandosi sullo stato dei booleani.
     */
    public static int currentLogMask() {
        int mask = LOG_NONE;
        if (LOG_STATUS_ENABLED)    mask |= LOG_STATUS;
        if (LOG_INIT_ENABLED)      mask |= LOG_INIT;
        if (LOG_BUS_ENABLED)       mask |= LOG_BUS;
        if (LOG_ALU_ENABLED)       mask |= LOG_ALU;
        if (LOG_REGISTERS_ENABLED) mask |= LOG_REGISTERS;
        if (LOG_INTERRUPT_ENABLED) mask |= LOG_INTERRUPT;
        if (LOG_PPU_ENABLED)       mask |= LOG_PPU;
        return mask;
    }

    /**
     * Aggiunge uno o più flag alla configurazione corrente senza spegnere gli altri.
     */
    public static void addLogging(int logMask) {
        applyLogMask(currentLogMask() | logMask); // Operatore OR unisce i bit
    }

    /**
     * Rimuove uno o più flag dalla configurazione corrente lasciando gli altri intatti.
     */
    public static void removeLogging(int logMask) {
        applyLogMask(currentLogMask() & ~logMask); // Operatore AND NOT spegne i bit scelti
    }

    public static void main(String[] args) {
    }
}