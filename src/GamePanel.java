import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

class GamePanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener {
    private static Image[] duckImages;
    private static Image bulletImage;
    private static Image heartImage;
    private final JFrame parentFrame;

    static {
        try {
            duckImages = new Image[] {
                    ImageIO.read(new File("src/images/duckRight0.png")),
                    ImageIO.read(new File("src/images/duckRight1.png"))
            };
            bulletImage = ImageIO.read(new File("src/images/bullet.png"));
            heartImage = ImageIO.read(new File("src/images/heart.png"));
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