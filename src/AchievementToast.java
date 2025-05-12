import javax.swing.*;
import java.awt.*;

class AchievementToast extends JWindow {
    private int offsetIndex = 0;

    public AchievementToast(JFrame parent, String text, int offsetIndex) {
        this.offsetIndex = offsetIndex;
        JLabel label = new JLabel("🎖 " + text);
        label.setOpaque(true);
        label.setBackground(new Color(30, 30, 30, 230));
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        add(label);
        pack();
        reposition(parent);
        setAlwaysOnTop(true);
        setVisible(true);

        new Timer(3000, e -> dispose()).start();
    }

    public void setOffset(int offset) {
        this.offsetIndex = offset;
    }

    public void reposition(JFrame parent) {
        Point location = parent.getLocationOnScreen();
        int x = location.x + parent.getWidth() - getWidth() - 30;
        int y = location.y + parent.getHeight() - getHeight() - 60 - (offsetIndex * (getHeight() + 10));
        setLocation(x, y);
    }
}