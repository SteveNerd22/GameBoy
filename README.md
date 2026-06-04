# SM83 Emulator

Un emulatore a basso livello dell'architettura Sharp SM83 (Game Boy DMG-01) sviluppato interamente da zero come passatempo.

Il progetto è scritto in Java puro e utilizza esclusivamente le API native del **JDK 21**, senza alcuna libreria o dipendenza esterna. L'obiettivo è emulare i singoli componenti hardware sincronizzandoli ciclo per ciclo rispetto al clock di sistema.

# 

---

## 📊 Stato di Avanzamento del Progetto

**Avanzamento Totale: ~25.5%**

| Componente                | Stato di Avanzamento        | Note / Dettagli                                                                                         |
|:--------------------------|:----------------------------|:--------------------------------------------------------------------------------------------------------|
| **CPU (SM83)**            | 🟨 80.4% (Base)             | Pipeline step-accurate funzionante. Mappati 206 / 256 opcode principali. ALU e IDU integrate.           |
| **MMU**                   | 🟨 20%                      | Mappatura completa dello spazio `0x0000-0xFFFF` tramite Enum.                                           |
| **PPU (Video)**           | 🟥 0%                       | Da iniziare.                                                                                            |
| **APU (Audio)**           | 🟥 0%                       | Da iniziare.                                                                                            |
| **Joypad**                | 🟥 0%                       | Da iniziare.                                                                                            |
| **Timer**                 | 🟥 0%                       | Da iniziare.                                                                                            |
| **Cartridge / MBC**       | 🟥 0%                       | Da iniziare                                                                                             |
