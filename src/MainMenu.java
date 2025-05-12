import javax.swing.*;
import java.awt.*;

class MainMenu extends JFrame {
    public MainMenu() {
        setTitle("Duck Hunt");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));

        JLabel label = new JLabel("Выберите уровень сложности:", SwingConstants.CENTER);
        String[] options = {"Лёгкий", "Средний", "Сложный"};
        JComboBox<String> combo = new JComboBox<>(options);
        combo.setSelectedIndex(1);

        JButton startBtn = new JButton("Начать игру");
        JButton rulesBtn = new JButton("Правила игры");
        JButton exitBtn = new JButton("Выход из игры");
        JButton achievementsBtn = new JButton("Достижения");

        startBtn.addActionListener(e -> { new GameFrame(combo.getSelectedIndex()); dispose(); });
        rulesBtn.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Правила игры:\n" +
                        "1) У вас 3 патрона и 5 жизней (сердечек).\n" +
                        "2) Кликайте по уткам или нажимайте W, чтобы их подбить (+1 очко).\n" +
                        "3) Собирайте патроны, чтобы получить +3 патрона.\n" +
                        "4) Если утка улетает за правый край экрана, вы теряете 1 жизнь.\n" +
                        "5) Игра заканчивается, когда кончаются патроны или жизни.\n" +
                        "6) Любой клик мышки или нажатие W тратит патрон.",
                "Правила игры", JOptionPane.INFORMATION_MESSAGE));
        exitBtn.addActionListener(e -> System.exit(0));
        achievementsBtn.addActionListener(e -> new AchievementsFrame());

        panel.add(label);
        panel.add(combo);
        panel.add(startBtn);
        panel.add(rulesBtn);
        panel.add(exitBtn);
        panel.add(achievementsBtn, 3);
        add(panel);
        setVisible(true);
    }
}