package org.example.cpu;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestRom {

    public static void main(String[] args) {
        // Path romPath = Paths.get("PokemonRed.gb");
        // Path romPath = Paths.get("06-ld r,r.gb");
        Path romPath = Paths.get("cpu_instrs.gb");

        InteractiveDebugger debugger = new InteractiveDebugger();

        System.out.println("🎮 Inizializzazione sessione di test per Pokémon Rosso...");
        debugger
                .withoutLogging()
                //.withInitLogging()
                .withMemoryLogging(false, true)
                .runNonStop()
                .onEnding(new EmulationEndingHandler() {
                    @Override
                    public void onExit(DebugContext ctx) {
                        System.out.println("\n==================================================");
                        System.out.println("📊 RESOCONTO FINALE AUTOMATIZZATO DI FINE CORSA");
                        System.out.println("==================================================");

                        // Sfruttiamo executeCommand per lanciare i comandi REPL via codice!
                        System.out.println("\n📋 [COMMAND EXEC] Verifichiamo lo stato finale dei registri:");
                        debugger.executeCommand("reg", ctx);

                        System.out.println("\n📜 [COMMAND EXEC] Diamo un'occhiata agli ultimi passi eseguiti (History):");
                        debugger.executeCommand("history", ctx);

                        // Possiamo anche fare asserzioni vecchio stile Java direttamente sull'hardware
                        System.out.println("\n🔬 [DIRECT CHECK] Controllo diretto hardware:");
                        int registroA = ctx.getCpu().A.get();
                        System.out.printf("   Il registro A è a: 0x%02X\n", registroA);
                        if (registroA == 0x00) {
                            System.out.println("   ✅ TEST PASSATO CON SUCCESSO!");
                        } else {
                            System.out.println("   ❌ TEST FALLITO! Rilevato comportamento anomalo.");
                        }
                        System.out.println("==================================================");
                    }

                    @Override
                    public void onError(DebugContext ctx, Throwable error) {
                        System.err.println("\n💥 CRASH EMULATORE RILEVATO DALL'HANDLER!");
                        System.err.println("Motivo: " + error.getMessage());
                        System.err.println("Dump dei registri al momento del crash:");
                        debugger.executeCommand("reg", ctx);
                    }
                })
                .start(romPath);
    }
}