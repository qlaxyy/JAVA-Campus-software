package edu.seu.vcampus.client.view;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Soft, rounded controls without the opaque rectangular Swing background. */
public final class RoundedButtonUI extends BasicButtonUI {
    @Override
    public void update(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color background = button.getBackground();
        if (button.getModel().isPressed()) {
            background = background.darker();
        } else if (button.getModel().isRollover()) {
            background = new Color(Math.max(0, background.getRed() - 8),
                    Math.max(0, background.getGreen() - 8), Math.max(0, background.getBlue() - 8));
        }
        copy.setColor(background);
        copy.fillRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, 18, 18);
        copy.setColor(new Color(140, 172, 166, button.isEnabled() ? 110 : 45));
        copy.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, 18, 18);
        if (button.hasFocus()) {
            copy.setColor(new Color(15, 118, 110));
            copy.drawRoundRect(2, 2, button.getWidth() - 5, button.getHeight() - 5, 15, 15);
        }
        copy.dispose();
        paint(graphics, component);
    }
}
