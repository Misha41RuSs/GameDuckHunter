import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import java.util.List;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu());
    }
}

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

// Обновлённый GamePanel с достижениями

// Обновлённый GamePanel с достижениями и максимальным счётом

// Обновлённый GamePanel с достижениями и максимальным счётом

class GamePanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener {
    private static Image[] duckImages;
    private static Image bulletImage;
    private static Image heartImage;
    private final JFrame parentFrame;

    static {
        try {
            duckImages = new Image[] {
                    ImageIO.read(new File("src/duckRight0.png")),
                    ImageIO.read(new File("src/duckRight1.png"))
            };
            bulletImage = ImageIO.read(new File("src/bullet.png"));
            heartImage = ImageIO.read(new File("src/heart.png"));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private final Timer timer;
    private final ArrayList<Duck> ducks = new ArrayList<>();
    private final ArrayList<Ammo> ammos = new ArrayList<>();
    private int bullets = 3;
    private int lives = 5;
    private int score = 0;
    private int killsMedium = 0;
    private int ammoCollected = 0;
    private int consecutiveMisses = 0;
    private boolean hitLast = true;
    private int scoreWithoutMiss = 0;
    private final Random random = new Random();
    private int mouseX, mouseY;

    private JLabel bulletsLabel;
    private JLabel scoreLabel;
    private JPanel livesPanel;

    private final int minSpeed, maxSpeed, maxDucks;
    private final int difficulty;

    private final AchievementManager achievementManager = new AchievementManager();

    private final File maxScoreFile = new File("max_score.txt");

    public GamePanel(int difficulty, JFrame parentFrame) {
        this.difficulty = difficulty;
        this.parentFrame = parentFrame;
        switch (difficulty) {
            case 0 -> { minSpeed = 5; maxSpeed = 10; maxDucks = 3; }
            case 2 -> { minSpeed = 12; maxSpeed = 18; maxDucks = 6; }
            default -> { minSpeed = 9; maxSpeed = 14; maxDucks = 5; }
        }
        setBackground(Color.CYAN);
        setFocusable(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        achievementManager.loadAchievements();
        achievementManager.setOnUnlockCallback(desc ->
            SwingUtilities.invokeLater(() -> ToastManager.showToast(parentFrame, "Открыто достижение: " + desc))
         );

        timer = new Timer(60, this);
        timer.start();
    }

    public void setInfoComponents(JLabel bulletsLabel, JLabel scoreLabel, JPanel livesPanel) {
        this.bulletsLabel = bulletsLabel;
        this.scoreLabel = scoreLabel;
        this.livesPanel = livesPanel;
        updateInfoPanel();
    }

    public int getBullets() { return bullets; }
    public int getScore() { return score; }

    private void updateInfoPanel() {
        if (bulletsLabel != null) bulletsLabel.setText("Патронов: " + bullets);
        if (scoreLabel != null) scoreLabel.setText("Очков: " + score);

        if (livesPanel != null) {
            livesPanel.removeAll();
            if (heartImage != null) {
                for (int i = 0; i < lives; i++) {
                    JLabel heartLabel = new JLabel(new ImageIcon(heartImage));
                    livesPanel.add(heartLabel);
                }
            } else {
                livesPanel.add(new JLabel("Жизней: " + lives));
            }
            livesPanel.revalidate();
            livesPanel.repaint();
        }
    }

    private void spawnDuck() {
        int h = getHeight(), w = getWidth();
        if (h < duckImages[0].getHeight(null) || w < duckImages[0].getWidth(null)) return;
        int y = random.nextInt(h - duckImages[0].getHeight(null));
        int speed = random.nextInt(maxSpeed - minSpeed + 1) + minSpeed;
        int dy = random.nextBoolean() ? speed/3 : -speed/3;
        ducks.add(new Duck(-duckImages[0].getWidth(null), y, speed, dy));
    }

    private void spawnAmmo() {
        int h = getHeight();
        if (h < bulletImage.getHeight(null)) return;
        int y = random.nextInt(h - bulletImage.getHeight(null));
        int speed = random.nextInt(3) + 2;
        ammos.add(new Ammo(-bulletImage.getWidth(null), y, speed));
    }

    private void shoot(Point p) {
        if (bullets <= 0) return;
        bullets--;
        updateInfoPanel();

        boolean hit = false;

        Iterator<Duck> dit = ducks.iterator();
        while (dit.hasNext()) {
            Duck d = dit.next();
            Rectangle r = new Rectangle(d.x, d.y, duckImages[0].getWidth(null), duckImages[0].getHeight(null));
            if (r.contains(p)) {
                dit.remove();
                score++;
                hit = true;
                if (difficulty == 1) {
                    killsMedium++;
                    if (killsMedium >= 10)
                        achievementManager.unlock("Убей 10 уток на средней сложности");
                }
                if (score >= 50)
                    achievementManager.unlock("Набери 50 очков на любой сложности");
                updateInfoPanel();
                break;
            }
        }

        Iterator<Ammo> ait = ammos.iterator();
        while (ait.hasNext()) {
            Ammo a = ait.next();
            Rectangle r2 = new Rectangle(a.x, a.y, bulletImage.getWidth(null), bulletImage.getHeight(null));
            if (r2.contains(p)) {
                ait.remove();
                bullets += 3;
                ammoCollected++;
                if (ammoCollected >= 5)
                    achievementManager.unlock("Собери 5 патронов");
                updateInfoPanel();
                hit = true;
                break;
            }
        }

        if (!hit) {
            consecutiveMisses++;
            scoreWithoutMiss = 0;
            if (consecutiveMisses >= 5)
                achievementManager.unlock("Промахнись 5 раз подряд");
        } else {
            consecutiveMisses = 0;
            scoreWithoutMiss++;
            if (scoreWithoutMiss >= 10)
                achievementManager.unlock("Попади 10 раз подряд без промахов");
        }

        checkGameOver();
    }

    private void checkGameOver() {
        if (bullets <= 0 || lives <= 0) {
            timer.stop();
            int max = loadMaxScore();
            if (score > max) saveMaxScore(score);

            String message = "Игра окончена! Ваш счёт: " + score;
            if (score > max) message += "\nНовый рекорд!";
            message += lives <= 0 ? "\nВы потеряли все жизни!" : "\nУ вас закончились патроны!";

            JOptionPane.showMessageDialog(this, message);
            SwingUtilities.invokeLater(MainMenu::new);
            SwingUtilities.getWindowAncestor(this).dispose();
        }
    }

    private int loadMaxScore() {
        try (Scanner sc = new Scanner(maxScoreFile)) {
            return sc.hasNextInt() ? sc.nextInt() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private void saveMaxScore(int value) {
        try (PrintWriter out = new PrintWriter(maxScoreFile)) {
            out.println(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int sunSize = 60;
        g.setColor(Color.YELLOW);
        g.fillOval(getWidth() - sunSize - 20, 20, sunSize, sunSize);
        for (Duck d : ducks) {
            g.drawImage(duckImages[d.frame], d.x, d.y,
                    duckImages[d.frame].getWidth(null),
                    duckImages[d.frame].getHeight(null), this);
        }
        for (Ammo a : ammos) {
            g.drawImage(bulletImage, a.x, a.y,
                    bulletImage.getWidth(null), bulletImage.getHeight(null), this);
        }
        g.setColor(Color.BLACK);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));
        int R1 = 20, R2 = 10;
        g2.drawOval(mouseX - R1, mouseY - R1, 2 * R1, 2 * R1);
        g2.drawOval(mouseX - R2, mouseY - R2, 2 * R2, 2 * R2);
        g2.drawLine(mouseX - R1 - 5, mouseY, mouseX + R1 + 5, mouseY);
        g2.drawLine(mouseX, mouseY - R1 - 5, mouseX, mouseY + R1 + 5);
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (ducks.size() < maxDucks) spawnDuck();
        if (ammos.size() < 2 && random.nextInt(100) < 5) spawnAmmo();

        int h = getHeight(), w = getWidth();
        Iterator<Duck> dit = ducks.iterator();
        while (dit.hasNext()) {
            Duck d = dit.next();
            d.x += d.speed;
            d.y += d.dy;
            if (d.y < 0 || d.y > h - duckImages[0].getHeight(null)) d.dy = -d.dy;
            d.frame = (d.frame + 1) % duckImages.length;
            if (d.x > w) {
                dit.remove();
                lives--;
                updateInfoPanel();
                checkGameOver();
            }
        }
        Iterator<Ammo> ait = ammos.iterator();
        while (ait.hasNext()) {
            Ammo a = ait.next();
            a.x += a.speed;
            if (a.x > w) ait.remove();
        }
        repaint();
    }

    @Override public void mouseClicked(MouseEvent e) { shoot(e.getPoint()); }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); repaint(); }
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) {
            shoot(new Point(mouseX, mouseY));
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
}



class Duck {
    int x, y, speed, dy, frame = 0;
    public Duck(int x, int y, int speed, int dy) { this.x = x; this.y = y; this.speed = speed; this.dy = dy; }
}

class Ammo {
    int x, y, speed;
    public Ammo(int x, int y, int speed) { this.x = x; this.y = y; this.speed = speed; }
}

class AchievementManager {
    private final File file = new File("achievements.txt");
    private final Map<String, Boolean> achievements = new LinkedHashMap<>();
    private Consumer<String> onUnlockCallback;

    public AchievementManager() {
        achievements.put("Убей 10 уток на средней сложности", false);
        achievements.put("Набери 50 очков на любой сложности", false);
        achievements.put("Собери 5 патронов", false);
        achievements.put("Промахнись 5 раз подряд", false);
        achievements.put("Попади 10 раз подряд без промахов", false);
    }

    public void setOnUnlockCallback(Consumer<String> callback) {
        this.onUnlockCallback = callback;
    }

    public void loadAchievements() {
        if (!file.exists()) return;
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (achievements.containsKey(line)) achievements.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unlock(String desc) {
        if (!achievements.containsKey(desc) || achievements.get(desc)) return;
        achievements.put(desc, true);
        saveAchievements();
        if (onUnlockCallback != null) onUnlockCallback.accept(desc);
    }

    public void saveAchievements() {
        try (PrintWriter out = new PrintWriter(file)) {
            for (var entry : achievements.entrySet()) {
                if (entry.getValue()) out.println(entry.getKey());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isUnlocked(String desc) {
        return achievements.getOrDefault(desc, false);
    }

    public Set<String> getAchievementsDescriptions() {
        return achievements.keySet();
    }
}

class ToastManager {
    private static final List<AchievementToast> activeToasts = new ArrayList<>();

    public static void showToast(JFrame parent, String text) {
        AchievementToast toast = new AchievementToast(parent, text, activeToasts.size());
        activeToasts.add(toast);
        toast.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                activeToasts.remove(toast);
                repositionToasts(parent);
            }
        });
        playSound();
    }

    private static void repositionToasts(JFrame parent) {
        for (int i = 0; i < activeToasts.size(); i++) {
            AchievementToast toast = activeToasts.get(i);
            toast.setOffset(i);
            toast.reposition(parent);
        }
    }

    private static void playSound() {
        try {
            InputStream in = AchievementToast.class.getResourceAsStream("/archivo.wav");
            if (in == null) {
                System.err.println("Файл не найден: archivo.wav");
                return;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

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