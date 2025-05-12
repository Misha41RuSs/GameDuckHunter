import javax.swing.*;
import java.awt.*;

class GameFrame extends JFrame {
    public GameFrame(int difficulty) {
        setTitle("Duck Hunt - Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Создаем панель для интерфейса и игровую панель
        JPanel interfacePanel = new JPanel(new BorderLayout());
        GamePanel gamePanel = new GamePanel(difficulty, this);

        // Панель для отображения информации (патроны, очки, жизни)
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(Color.LIGHT_GRAY);
        infoPanel.setPreferredSize(new Dimension(800, 40));

        // Создаем метки для отображения информации
        JLabel bulletsLabel = new JLabel("Патронов: " + gamePanel.getBullets());
        JLabel scoreLabel = new JLabel("Очков: " + gamePanel.getScore());
        JPanel livesPanel = new JPanel();
        livesPanel.setBackground(Color.LIGHT_GRAY);

        // Добавляем компоненты на infoPanel
        infoPanel.add(bulletsLabel);
        infoPanel.add(scoreLabel);
        infoPanel.add(livesPanel);

        // Добавляем панели на основной интерфейс
        interfacePanel.add(infoPanel, BorderLayout.NORTH);
        interfacePanel.add(gamePanel, BorderLayout.CENTER);

        // Устанавливаем связь между игровой панелью и панелью информации
        gamePanel.setInfoComponents(bulletsLabel, scoreLabel, livesPanel);

        add(interfacePanel);
        pack();
        setSize(800, 640); // Увеличиваем высоту на 40px для infoPanel
        setLocationRelativeTo(null);
        setVisible(true);
        gamePanel.requestFocusInWindow();
    }
}