package org.example.cpu;

import org.example.GameBoy;
import org.example.Main;
import org.example.clock.PulseListener;
import org.example.bus.data.InterruptSignal; // 🔥 Assicurati che questo import sia corretto
import org.example.cpu.commands.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class InteractiveDebugger {
    private final Map<String, DebugCommand> commands = new HashMap<>();
    private final List<PulseListener> customPulseListeners = new ArrayList<>();
    private EmulationEndingHandler endingHandler = null;
    private boolean nonStopMode = false;
    private boolean standardErrorRedirect = false;
    private String errorLogPath;
    private TeePrintStream teeStream;

    public InteractiveDebugger() {
        registerCommand(new RegCommand());
        registerCommand(new BreakpointCommand());
        registerCommand(new HistoryCommand());
        registerCommand(new VramCommand());
        registerCommand(new MmuCommand());

        Main.applyLogMask(Main.LOG_STATUS);
    }

    private void registerCommand(DebugCommand cmd) {
        commands.put(cmd.getKeyword(), cmd);
    }

    public InteractiveDebugger addPulseListener(PulseListener listener) {
        this.customPulseListeners.add(listener);
        return this;
    }

    public InteractiveDebugger withAllLogging() {
        Main.applyLogMask(Main.LOG_ALL);
        return this;
    }

    public InteractiveDebugger withoutLogging() {
        Main.applyLogMask(Main.LOG_NONE);
        return this;
    }

    public InteractiveDebugger withBusLogging() {
        Main.LOG_BUS_ENABLED = true;
        return this;
    }

    public InteractiveDebugger withInitLogging() {
        Main.LOG_INIT_ENABLED = true;
        return this;
    }

    public InteractiveDebugger withLogging(int logMask) {
        Main.applyLogMask(logMask);
        return this;
    }

    public InteractiveDebugger addLogging(int logMask) {
        Main.addLogging(logMask);
        return this;
    }

    public InteractiveDebugger removeLogging(int logMask) {
        Main.removeLogging(logMask);
        return this;
    }

    /**
     * Configura l'handler da invocare al termine dell'emulazione.
     */
    public InteractiveDebugger onEnding(EmulationEndingHandler handler) {
        this.endingHandler = handler;
        return this;
    }

    /**
     * Copia tutto lo Standard Error (compresi i crash e i dump) su un file dedicato.
     * Mantiene la console invariata.
     */
    public InteractiveDebugger copyErrorsToFile(String path) {
        this.errorLogPath = path;
        this.standardErrorRedirect = false;
        return this;
    }

    /**
     * Redirige tutto lo Standard Error (compresi i crash e i dump) su un file dedicato.
     * Mantiene la console pulita per lo standard output o per l'esecuzione silenziosa.
     */
    public InteractiveDebugger redirectErrorsToFile(String path) {
        this.errorLogPath = path;
        this.standardErrorRedirect = true;
        return this;
    }

    /**
     * Attiva la modalità ad avanzamento continuo. Il debugger eseguirà i cicli
     * all'infinito (o finché non incontra un breakpoint) senza mai chiedere l'input utente.
     */
    public InteractiveDebugger runNonStop() {
        this.nonStopMode = true;
        return this;
    }

    /**
     * Permette di eseguire un comando del debugger programmaticamente via codice.
     * Molto utile negli handler di fine corsa o nei listener.
     */
    public void executeCommand(String input, DebugContext ctx) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return;

        String[] parts = trimmed.split("\\s+");
        String keyword = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        if (commands.containsKey(keyword)) {
            DebugCommand cmd = commands.get(keyword);
            cmd.execute(ctx, args);
        } else {
            System.out.println("⚠️ Comando programmatico sconosciuto: " + keyword);
        }
    }

    public void start(Path romPath) {
        start(romPath, 0x0100);
    }

    public void start(Path romPath, int initialPC) {
        if (!Files.exists(romPath)) {
            System.err.println("❌ ERRORE: ROM non trovata.");
            return;
        }

        if (this.errorLogPath != null) {
            try {
                java.io.PrintStream fileOut = new java.io.PrintStream(
                        new java.io.FileOutputStream(this.errorLogPath, false)
                );
                if (this.standardErrorRedirect) {
                    System.setErr(fileOut);
                } else {
                    teeStream = new TeePrintStream(System.err, fileOut);
                    System.setErr(teeStream);
                }
            } catch (java.io.FileNotFoundException e) {
                System.out.println("⚠️ Impossibile creare il file di log degli errori: " + this.errorLogPath);
            }
        }

        DebugContext ctx = null;

        try {
            byte[] rawBytes = Files.readAllBytes(romPath);
            int[] romData = new int[rawBytes.length];
            for (int i = 0; i < rawBytes.length; i++) romData[i] = rawBytes[i] & 0xFF;

            GameBoy gameBoy = new GameBoy();
            gameBoy.getMmu().loadCartridge(romData);
            gameBoy.setDebuggerControlled(true);

            if (Main.LOG_BUS_ENABLED) {
                setupBusReaders(gameBoy);
            }

            SM83 cpu = gameBoy.getCpu();
            setupInitialState(cpu, initialPC);

            ctx = new DebugContext(gameBoy);

            setupPulseListeners(gameBoy, ctx);

            gameBoy.turnOn();

            if (this.nonStopMode) {
                System.out.printf("🚀 MODALITÀ CONTINUA ATTIVA (PC: 0x%04X). L'emulatore sta correndo...\n", initialPC);

                while (ctx.isRunning()) {
                    eseguiStep(gameBoy, ctx, 1);

                    if (ctx.shouldBreakAt(gameBoy.getCpu().PC.get())) {
                        System.out.println("🛑 Emulazione interrotta per breakpoint.");
                        break;
                    }
                }
            } else {
                // ⌨️ MODALITÀ REPL STANDARD (Interattiva con input utente)
                System.out.printf("🔥 REPL DEBUGGER PRONTO (PC: 0x%04X). Scrivi un comando o premi Invio.\n", initialPC);
                Scanner scanner = new Scanner(System.in);

                while (ctx.isRunning()) {
                    System.out.print("dbg> ");
                    String input = scanner.nextLine().trim();

                    if (input.equalsIgnoreCase("stop")) {
                        ctx.stop();
                        break;
                    }

                    if (input.isEmpty()) {
                        eseguiStep(gameBoy, ctx, 1);
                        continue;
                    }

                    String[] parts = input.split("\\s+");
                    String keyword = parts[0].toLowerCase();
                    String[] args = Arrays.copyOfRange(parts, 1, parts.length);

                    if (commands.containsKey(keyword)) {
                        DebugCommand cmd = commands.get(keyword);
                        cmd.execute(ctx, args);
                    } else {
                        try {
                            int n = Integer.parseInt(keyword);
                            eseguiStep(gameBoy, ctx, n);
                        } catch (NumberFormatException e) {
                            System.out.println("⚠️ Comando sconosciuto.");
                        }
                    }
                }
            }

            if (endingHandler != null) {
                endingHandler.onExit(ctx);
            }

        } catch (Throwable e) {
            if (endingHandler != null && ctx != null) {
                System.out.flush();
                java.io.PrintStream originalOut = System.out;
                try {
                    System.setOut(System.err);
                    endingHandler.onError(ctx, e);
                } finally {
                    System.setOut(originalOut);
                    System.err.close();
                }
            } else {
                System.err.println("Anomalia rilevata senza handler configurato:");
                e.printStackTrace();
            }
        } finally {
            System.out.println("\n👋 Sessione di emulazione conclusa.");
            System.exit(0);
        }
    }

    private void eseguiStep(GameBoy gb, DebugContext ctx, int totalMCycles) {
        for (int i = 0; i < totalMCycles; i++) {
            if (ctx.shouldBreakAt(ctx.getCpu().PC.get())) {
                System.out.printf("\n🛑 BREAKPOINT COLPITO a 0x%04X all'M-Cycle %d di %d!\n",
                        ctx.getCpu().PC.get(), i, totalMCycles);
                break;
            }
            gb.stepMachineCycle();
        }
    }

    private void setupPulseListeners(GameBoy gameBoy, DebugContext ctx) {
        final int[] tTickCounter = {0};
        final long[] mCyclesExecuted = {0};

        gameBoy.RegisterPulseListener(new PulseListener() {
            @Override
            public void onPulse() {
                tTickCounter[0]++;
                if (tTickCounter[0] >= 4) {
                    tTickCounter[0] = 0;
                    mCyclesExecuted[0]++;

                    SM83 cpu = ctx.getCpu();
                    int currentPC = cpu.PC.get();
                    int opcode = gameBoy.getMmu().readByte(currentPC, cpu);

                    String statusLog = String.format(
                            "M-Cycle: %-5d | PC: 0x%04X | Opcode: 0x%02X | IR: 0x%02X | A: 0x%02X | HL: 0x%04X",
                            mCyclesExecuted[0], currentPC, opcode, cpu.IR.get(), cpu.A.get(), cpu.HL.get()
                    );
                    if (Main.LOG_STATUS_ENABLED) {
                        System.out.println(statusLog);
                    }
                    ctx.addLogEntry(statusLog);
                }
            }
        });

        for (PulseListener customListener : customPulseListeners) {
            gameBoy.RegisterPulseListener(customListener);
        }
    }

    // 🆕 METODO PRIVATO: Racchiude la logica di logging del Bus per non sporcare il metodo start()
    private void setupBusReaders(GameBoy gameBoy) {
        SM83 cpu = gameBoy.getCpu();

        cpu.SoCAddress.registerReader((sender, data) -> {
            if (Main.LOG_BUS_ENABLED) {
                System.out.printf("[BUS-ADDR] Mittente: %-12s | Indirizzo: 0x%04X\n",
                        sender.getClass().getSimpleName(), data.getAddress());
            }
        });

        cpu.SoCData.registerReader((sender, data) -> {
            if (Main.LOG_BUS_ENABLED) {
                System.out.printf("[BUS-DATA] Mittente: %-12s | Dato: 0x%02X\n",
                        sender.getClass().getSimpleName(), data.getByteValue());
            }
        });

        cpu.SoCInterrupts.registerReader((sender, data) -> {
            if (Main.LOG_BUS_ENABLED) {
                int mask = data.getBitMask();
                String signalType = "INTERRUPT (0x" + String.format("%04X", mask) + ")";

                if (mask == InterruptSignal.MEM_RD) signalType = "CONTROL: MEM_RD 📖";
                else if (mask == InterruptSignal.MEM_WR) signalType = "CONTROL: MEM_WR ✍️";
                else if (mask == InterruptSignal.NONE) signalType = "NONE 🛑";

                System.out.printf("[BUS-CTRL] Mittente: %-12s | Segnale: %s\n",
                        sender.getClass().getSimpleName(), signalType);
            }
        });
    }

    private void setupInitialState(SM83 cpu, int initialPC) {
        cpu.PC.set(initialPC);
        if (initialPC == 0x0100) {
            cpu.SP.set(0xFFFE);
            cpu.A.set(0x01);    cpu.F.set(0xB0);
            cpu.B.set(0x00);    cpu.C.set(0x13);
            cpu.D.set(0x00);    cpu.E.set(0xD8);
            cpu.H.set(0x01);    cpu.L.set(0x4D);
        } else {
            cpu.SP.set(0x0000);
            cpu.A.set(0x00);    cpu.F.set(0x00);
            cpu.B.set(0x00);    cpu.C.set(0x00);
            cpu.D.set(0x00);    cpu.E.set(0x00);
            cpu.H.set(0x00);    cpu.L.set(0x00);
        }
    }

    private static class TeePrintStream extends java.io.PrintStream {
        private final java.io.PrintStream secondStream;

        public TeePrintStream(java.io.OutputStream main, java.io.PrintStream second) {
            super(main, true); // true = auto-flush attivo
            this.secondStream = second;
        }

        @Override
        public void write(int b) {
            super.write(b);
            secondStream.write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            secondStream.write(buf, off, len);
        }

        @Override
        public void flush() {
            super.flush();
            secondStream.flush();
        }

        @Override
        public void close() {
            super.close();
            secondStream.close();
        }
    }
}