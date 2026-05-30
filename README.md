# SM83 Emulator

Un emulatore a basso livello dell'architettura Sharp SM83 (Game Boy DMG-01) sviluppato interamente da zero come passatempo.

Il progetto è scritto in Java puro e utilizza esclusivamente le API native del **JDK 21**, senza alcuna libreria o dipendenza esterna. L'obiettivo è emulare i singoli componenti hardware sincronizzandoli ciclo per ciclo rispetto al clock di sistema.

# 

---

## 📊 Stato di Avanzamento del Progetto

**Avanzamento Totale: ~8.3%**

| Componente                | Stato di Avanzamento | Note / Dettagli                                               |
|:--------------------------|:---------------------|:--------------------------------------------------------------|
| **CPU (SM83)**            | 🟨 30%               | Pipeline a stati funzionante. Mappate 4 istruzioni totali.    |
| **MMU**                   | 🟨 20%               | Mappatura completa dello spazio `0x0000-0xFFFF` tramite Enum. |
| **PPU (Video)**           | 🟥 0%                | Da iniziare.                                                  |
| **APU (Audio)**           | 🟥 0%                | Da iniziare.                                                  |
| **Joypad**                | 🟥 0%                | Da iniziare.                                                  |
| **Timer**                 | 🟥 0%                | Da iniziare.                                                  |
| **Cartridge / MBC**       | 🟥 0%                | Da iniziare                                                   |

#

___

## 🚀 Test della Pipeline Corrente

Il sistema esegue correttamente una sequenza di test pre-caricata in memoria, mostrando il passaggio asincrono dei dati nei registri e l'avanzamento del Program Counter (PC) coordinato con gli stati interni della CPU:

```text
Tick 01 | Pipeline: FETCH   | PC: 0x0001 | A: 0x00 | B: 0x00  <-- NOP
Tick 02 | Pipeline: FETCH   | PC: 0x0002 | A: 0x00 | B: 0x00
Tick 03 | Pipeline: DECODE  | PC: 0x0002 | A: 0x00 | B: 0x00
Tick 04 | Pipeline: EXECUTE | PC: 0x0003 | A: 0x00 | B: 0x00
Tick 05 | Pipeline: EXECUTE | PC: 0x0003 | A: 0x42 | B: 0x00  <-- Eseguito: LD A, 0x42
Tick 06 | Pipeline: FETCH   | PC: 0x0004 | A: 0x42 | B: 0x00
Tick 07 | Pipeline: DECODE  | PC: 0x0004 | A: 0x42 | B: 0x00
Tick 08 | Pipeline: EXECUTE | PC: 0x0004 | A: 0x42 | B: 0x42  <-- Eseguito: LD B, A
```