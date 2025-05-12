import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
            InputStream in = AchievementToast.class.getResourceAsStream("/sounds/archivo.wav");
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
