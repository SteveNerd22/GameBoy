package org.example.cpu;

import org.example.Main;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestPokemonRed {

    public static void main(String[] args) {
        Path romPath = Paths.get("PokemonRed.gb");
        // Path romPath = Paths.get("06-ld r,r.gb");

        System.out.println("🎮 Inizializzazione sessione di test per Pokémon Rosso...");
        new InteractiveDebugger()
                //.enableBusLogging()
                .start(romPath);
    }
}