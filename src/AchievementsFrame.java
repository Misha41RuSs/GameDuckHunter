import javax.swing.*;
import java.awt.*;

class AchievementsFrame extends JFrame {
    public AchievementsFrame() {
        setTitle("Достижения");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        AchievementManager manager = new AchievementManager();
        manager.loadAchievements();

        for (String desc : manager.getAchievementsDescriptions()) {
            boolean unlocked = manager.isUnlocked(desc);
            JLabel label = new JLabel((unlocked ? "✅ " : "🔒 ") + desc);
            panel.add(label);
        }

        add(new JScrollPane(panel));
        setVisible(true);
    }
}