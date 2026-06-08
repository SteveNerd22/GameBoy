package org.example;

public class Clock {
    // La frequenza esatta del Game Boy DMG (4.194304 MHz)
    public static final int CLOCK_SPEED = 4_194_304;
    // Quanti T-cycles ci sono in un singolo frame (~60 FPS -> 4194304 / 59.73)
    public static final int CYCLES_PER_FRAME = 70_224;
    // Durata target BASE di un frame in nanosecondi (~16.74 ms a 100% di velocità)
    private static final long BASE_FRAME_TIME_NS = 16_742_706;

    private final GameBoy gameBoy;
    private boolean running = false;

    // Gestione della velocità dinamica
    private int speedPercentage = 100;
    private long currentFrameTimeNs = BASE_FRAME_TIME_NS;

    public Clock(GameBoy gameBoy) {
        this.gameBoy = gameBoy;
    }

    public void start() {
        if (running) return;
        running = true;

        Thread clockThread = new Thread(this::runLoop, "GB-Clock-Thread");
        clockThread.start();
    }

    public void stop() {
        this.running = false;
    }

    private void runLoop() {
        long lastFrameTime = System.nanoTime();

        while (running) {
            // Eseguiamo un blocco di cicli pari a un intero frame video
            for (int i = 0; i < CYCLES_PER_FRAME; i++) {
                gameBoy.pulseComponents();
            }

            if (speedPercentage > 0) {
                long currentFrameTime = System.nanoTime();
                long elapsedTime = currentFrameTime - lastFrameTime;

                // Usiamo il tempo del frame corrente ricalcolato in base alla percentuale
                long sleepTime = currentFrameTimeNs - elapsedTime;

                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            lastFrameTime = System.nanoTime();
        }
    }

    /**
     * Modifica la velocità dell'emulatore in tempo reale.
     * @param percentage Percentuale di velocità (es. 100 = normale, 200 = doppio, 50 = metà).
     * Impostare a 0 o valori negativi per sbloccare completamente il framerate.
     */
    public synchronized void setSpeedPercentage(int percentage) {
        if (percentage < 0) {
            percentage = 0;
        }

        this.speedPercentage = percentage;

        if (percentage > 0) {
            this.currentFrameTimeNs = (BASE_FRAME_TIME_NS * 100) / percentage;
        }
    }

    public int getSpeedPercentage() {
        return this.speedPercentage;
    }
}