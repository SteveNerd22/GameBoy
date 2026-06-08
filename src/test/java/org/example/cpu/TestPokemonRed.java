package org.example.cpu;

import org.example.GameBoy;
import org.example.bus.data.InterruptSignal;
import org.example.mmu.MMU;
import org.example.mmu.PhysicalMemory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class TestPokemonRed {

    public static void main(String[] args) {
        // Path romPath = Paths.get("PokemonRed.gb");
        Path romPath = Paths.get("06-ld r,r.gb");

        if (!Files.exists(romPath)) {
            System.err.println("❌ ERRORE: ROM di Pokemon Rosso non trovata.");
            return;
        }

        try {
            byte[] rawBytes = Files.readAllBytes(romPath);
            int[] romData = new int[rawBytes.length];
            for (int i = 0; i < rawBytes.length; i++) {
                romData[i] = rawBytes[i] & 0xFF;
            }

            // 1. Setup del GameBoy
            GameBoy gameBoy = new GameBoy();

            gameBoy.getCpu().SoCAddress.registerReader((sender, data) -> System.out.printf("[BUS-ADDR] Mittente: %-12s | Indirizzo: 0x%04X\n",
                    sender.getClass().getSimpleName(),
                    data.getAddress()
            ));

            gameBoy.getCpu().SoCData.registerReader((sender, data) -> System.out.printf("[BUS-DATA] Mittente: %-12s | Dato: 0x%02X\n",
                    sender.getClass().getSimpleName(),
                    data.getByteValue()
            ));

            gameBoy.getCpu().SoCInterrupts.registerReader((sender, data) -> {
                int mask = data.getBitMask();

                // Traduzione al volo dei tuoi segnali di controllo per rendere la console leggibile
                String signalType = "INTERRUPT (0x" + String.format("%04X", mask) + ")";
                if (mask == InterruptSignal.MEM_RD) signalType = "CONTROL: MEM_RD 📖";
                else if (mask == InterruptSignal.MEM_WR) signalType = "CONTROL: MEM_WR ✍️";
                else if (mask == InterruptSignal.NONE) signalType = "NONE 🛑";

                System.out.printf("[BUS-CTRL] Mittente: %-12s | Segnale: %s\n",
                        sender.getClass().getSimpleName(),
                        signalType
                );
            });



            // 🛑 SCOLLEGHIAMO IL QUARZO INTERNO REALE: Il debugger prende il controllo totale del tempo
            gameBoy.setDebuggerControlled(true);

            // 2. Carica la cartuccia nella MMU
            MMU mmu = gameBoy.getMmu();
            mmu.loadCartridge(romData);

            // 3. Setup dello stato iniziale dei registri della CPU
            SM83 cpu = gameBoy.getCpu();
            cpu.PC.set(0x0100);
            cpu.SP.set(0xFFFE);
            cpu.A.set(0x01); cpu.F.set(0xB0);
            cpu.B.set(0x00); cpu.C.set(0x13);
            cpu.D.set(0x00); cpu.E.set(0xD8);
            cpu.H.set(0x01); cpu.L.set(0x4D);

            // 4. Registriamo il listener di log (riceve le notifiche dei nostri impulsi manuali)
            final long[] mCyclesExecuted = {0};
            final int[] tTickCounter = {0};

            gameBoy.RegisterPulseListener(() -> {
                tTickCounter[0]++;
                if (tTickCounter[0] >= 4) {
                    tTickCounter[0] = 0;
                    mCyclesExecuted[0]++;

                    int currentPC = cpu.PC.get();
                    int opcode = mmu.readByte(currentPC, cpu);

                    System.out.printf(
                            "M-Cycle: %-5d | PC: 0x%04X | Opcode: 0x%02X | IR: 0x%02X | A: 0x%02X | HL: 0x%04X | Flags: %s\n",
                            mCyclesExecuted[0], currentPC, opcode, cpu.IR.get(), cpu.A.get(), cpu.HL.get(), getFlagsString(cpu.F.get())
                    );
                }
            });

            // Mostriamo la finestra Swing (rimarrà in attesa dei frame che gli manderemo noi)
            gameBoy.turnOn();

            System.out.println("🔥 IL DEBUGGER ESTERNO COMINCIA A SPINGERE I TICK MANUALMENTE...");
            System.out.println("=======================================================================================");

            // 5. IL LOOP SINCRO DEL DEBUGGER:
            // Inviamo noi gli impulsi. Puoi farlo girare per un numero fisso di cicli,
            // oppure farlo andare all'infinito. Non andrà mai "troppo veloce" perché
            // è vincolato alla velocità del thread principale corrente e dei System.out.printf.

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            do {
                int cicliDaEseguire = 0; // Di base non avanziamo se l'utente lancia un comando ispettivo
                boolean avanzaIlTempo = false;

                System.out.print("dbg> ");
                String input = scanner.nextLine().trim().toLowerCase();

                if (input.isEmpty()) {
                    // Invio semplice = avanza di 1 ciclo
                    cicliDaEseguire = 1;
                    avanzaIlTempo = true;
                } else if (input.equals("stop")) {
                    running = false;
                    System.out.println("🛑 Emulazione interrotta dal debugger.");
                    break;
                }
                // --- COMANDO 1: STAMPA REGISTRI CPU ---
                else if (input.equals("reg")) {
                    System.out.println("\n--- 🧠 STATO REALE CPU ---");
                    System.out.printf("PC: 0x%04X  |  SP: 0x%04X  |  IR: 0x%02X\n", cpu.PC.get(), cpu.SP.get(), cpu.IR.get());
                    System.out.printf("A:  0x%02X    |  F:  0x%02X (%s)\n", cpu.A.get(), cpu.F.get(), getFlagsString(cpu.F.get()));
                    System.out.printf("B:  0x%02X    |  C:  0x%02X   => BC: 0x%04X\n", cpu.B.get(), cpu.C.get(), (cpu.B.get() << 8) | cpu.C.get());
                    System.out.printf("D:  0x%02X    |  E:  0x%02X   => DE: 0x%04X\n", cpu.D.get(), cpu.E.get(), (cpu.D.get() << 8) | cpu.E.get());
                    System.out.printf("H:  0x%02X    |  L:  0x%02X   => HL: 0x%04X\n", cpu.H.get(), cpu.L.get(), cpu.HL.get());
                    System.out.println("---------------------------\n");
                }
                // --- COMANDO 2: ISPEZIONE VRAM (Solo celle scritte) ---
                else if (input.equals("vram")) {
                    System.out.println("\n--- 📺 ISPEZIONE VRAM DELLA PPU (Solo byte diversi da 0) ---");
                    int count = 0;
                    // Chiediamo i dati direttamente alla VRAM interna della PPU
                    // Nota: Assicurati di aggiungere un getter 'public int[] getVram()' dentro la tua classe PPU!
                    PhysicalMemory ppuVram = gameBoy.getPpu().getVram();

                    for (int addr = 0x0000; addr < ppuVram.getSize(); addr++) {
                        if (ppuVram.read(gameBoy.getPpu(), addr) != 0) {
                            System.out.printf("VRAM Indirizzo Corrente [0x%04X] (Mappa: 0x%04X) -> Valore: 0x%02X\n",
                                    addr, addr + 0x8000, ppuVram.read(gameBoy.getPpu(), addr));
                            count++;
                        }
                    }
                    if (count == 0) System.out.println("La VRAM è attualmente completamente vuota (tutti 0x00).");
                    System.out.println("-----------------------------------------------------------\n");
                }
                // --- COMANDO 3: ISPEZIONE PUNTO MMU DINAMICO (es: mmu c000) ---
                else if (input.startsWith("mmu ")) {
                    try {
                        String addrStr = input.substring(4).replace("0x", "").trim();
                        int targetAddr = Integer.parseInt(addrStr, 16);

                        System.out.printf("\n--- 💾 ISPEZIONE MMU INTORNO A 0x%04X ---\n", targetAddr);
                        // Leggiamo una piccola finestra di 8 byte prima e dopo per avere contesto
                        for (int i = -4; i <= 4; i++) {
                            int currentAddress = (targetAddr + i) & 0xFFFF;
                            String pointer = (i == 0) ? " => " : "    ";
                            System.out.printf("%s[0x%04X] -> 0x%02X\n", pointer, currentAddress, mmu.readByte(currentAddress, gameBoy.getCpu()));
                        }
                        System.out.println("-------------------------------------------\n");
                    } catch (Exception e) {
                        System.out.println("⚠️ Indirizzo non valido. Usa il formato esadecimale (es: mmu C000 o mmu 100)");
                    }
                }
                // --- CASO DEFAULT: AVANZAMENTO DI 'N' CICLI ---
                else {
                    try {
                        int n = Integer.parseInt(input);
                        if (n > 0) {
                            cicliDaEseguire = n;
                            avanzaIlTempo = true;
                        } else {
                            System.out.println("⚠️ Numero non valido.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("⚠️ Comando sconosciuto. Premi Invio per fare 1 passo, o digita 'reg', 'vram', 'mmu [indirizzo]'.");
                    }
                }

                // Eseguiamo i passi solo se l'utente ha inserito un comando di avanzamento temporale
                if (avanzaIlTempo) {
                    for (int i = 0; i < cicliDaEseguire; i++) {
                        gameBoy.stepMachineCycle();
                    }
                }

            } while (running);
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    private static String getFlagsString(int f) {
        return String.format("[%s%s%s%s]",
                (f & 0x80) != 0 ? "Z" : "-", (f & 0x40) != 0 ? "N" : "-", (f & 0x20) != 0 ? "H" : "-", (f & 0x10) != 0 ? "C" : "-");
    }
}
