package org.example;

import org.example.bus.AddressBus;
import org.example.bus.DataBus;
import org.example.bus.InterruptBus;
import org.example.clock.PulseListener;
import org.example.cpu.SM83;
import org.example.mmu.MMU;
import org.example.ppu.PPU;
import org.example.Clock;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GameBoy {

    private final AddressBus addressBus;
    private final DataBus dataBus;
    private final InterruptBus interruptBus;
    private final MMU mmu;
    private final SM83 cpu;
    private final PPU ppu;
    private final GameBoyWindow window;
    private final JFrame frame;
    private final Clock clock;
    private boolean isDebuggerControlled = false;

    private final List<PulseListener> pulseListeners;

    public GameBoy() {
        // 1. STRATO INFRASTRUTTURA: Crei le piste di rame (Bus)
        this.addressBus = new AddressBus();
        this.dataBus = new DataBus();
        this.interruptBus = new InterruptBus();

        // 2. STRATO MEMORIA: La MMU si collega ai bus. Ora la memoria ESISTE nel sistema.
        this.mmu = new MMU(this.addressBus, this.dataBus, this.interruptBus);

        // 3. STRATO CO-PROCESSORI: Crei la PPU che gestisce la sua VRAM/OAM isolata
        this.ppu = new PPU(interruptBus, dataBus, addressBus, mmu.getVram(), mmu.getOam());

        // 4. STRATO CPU: Crei il processore principale, che ora può dialogare con MMU e PPU sui bus
        this.cpu = new SM83(this.interruptBus, this.dataBus, this.addressBus);

        // 5. STRATO GRAFICO (GUI): Crei la finestra passando la PPU che ora è totalmente inizializzata
        this.window = new GameBoyWindow(ppu, 3);

        // 6. STRATO TIMING: Il clock si aggancia al sistema
        this.clock = new Clock(this);

        // 7. INIZIALIZZAZIONE JFRAME SWING
        this.frame = new JFrame("Java Game Boy Emulator");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.add(this.window);
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setResizable(false);

        this.pulseListeners = new ArrayList<>();
    }

    /**
     * Attiva o disattiva il controllo del clock da parte del debugger esterno.
     * Se impostato a true, il clock in tempo reale viene ignorato/fermato.
     */
    public void setDebuggerControlled(boolean controlled) {
        this.isDebuggerControlled = controlled;
        if (controlled) {
            this.clock.stop(); // Ferma il thread real-time se stava girando
        }
    }

    /**
     * Interfaccia pubblica per far partire l'emulazione
     */
    public void turnOn() {
        showWindow();
        if (!isDebuggerControlled) {
            clock.start();
        }
    }

    /**
     * Metodo chiamato dal Clock ad ogni singolo T-Tick.
     * Manda l'impulso di clock in parallelo ai componenti sincronizzati.
     */
    protected void pulseComponents() {
        cpu.pulse();
        ppu.pulse();

        // Controllo del V-Blank per aggiornare la finestra Swing
        if (ppu.getLy() == 144 && ppu.getTCyclesCounter() == 0) {
            this.window.repaint();
        }

        for(PulseListener listener: pulseListeners) {
            listener.onPulse();
        }
    }

    public void RegisterPulseListener(PulseListener listener) {
        if(!pulseListeners.contains(listener))
            this.pulseListeners.add(listener);
    }

    private void showWindow() {
        SwingUtilities.invokeLater(() -> this.frame.setVisible(true));
    }

    public SM83 getCpu() { return this.cpu; }
    public MMU getMmu() { return this.mmu; }
    public PPU getPpu() { return this.ppu; }
    public GameBoyWindow getWindow() { return this.window; }

    public void setSpeedPercentage(int percentage) {
        clock.setSpeedPercentage(percentage);
    }

    public void reset() {
        this.cpu.reset();
    }

    /**
     * IL METODO DEL DEBUGGER ESTERNO: Spinge un singolo T-Tick (impulso hardware)
     * in maniera completamente sincrona a tutto il sistema.
     */
    public void step() {
        // Questo esegue esattamente un tick di clock atomico
        this.pulseComponents();
    }

    /**
     * Comodità per il debugger: fa avanzare il sistema di un intero Ciclo Macchina (4 T-Tick)
     */
    public void stepMachineCycle() {
        for (int i = 0; i < 4; i++) {
            this.pulseComponents();
        }
    }
}