import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;

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