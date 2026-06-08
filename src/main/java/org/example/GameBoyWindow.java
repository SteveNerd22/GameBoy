package org.example;

import org.example.ppu.PPU;

import javax.swing.*;
import java.awt.*;

public class GameBoyWindow extends JPanel {
    private final PPU ppu;
    private final int scale;

    public GameBoyWindow(PPU ppu, int scale) {
        this.ppu = ppu;
        this.scale = scale;
        setPreferredSize(new Dimension(PPU.SCREEN_WIDTH * scale, PPU.SCREEN_HEIGHT * scale));
        this.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(ppu.getScreenImage(), 0, 0, PPU.SCREEN_WIDTH * scale, PPU.SCREEN_HEIGHT * scale, null);
    }
}