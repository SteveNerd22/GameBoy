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

    public void start(Path romPath) {
        start(romPath, 0x0100);
    }

    public void start(Path romPath, int initialPC) {
        if (!Files.exists(romPath)) {
            System.err.println("❌ ERRORE: ROM non trovata.");
            return;
        }

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

            DebugContext ctx = new DebugContext(gameBoy);

            setupPulseListeners(gameBoy, ctx);

            gameBoy.turnOn();
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
                        System.out.println("⚠️ Comando sconosciuto. Comandi disponibili:");
                        commands.values().forEach(c -> System.out.printf("  %-10s : %s\n", c.getKeyword(), c.getHelp()));
                    }
                }
            }

            System.exit(0);
        } catch (IOException e) {
            System.err.println("Errore di I/O: " + e.getMessage());
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
}