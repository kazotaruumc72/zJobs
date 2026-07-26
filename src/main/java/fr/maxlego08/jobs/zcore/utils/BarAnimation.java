package fr.maxlego08.jobs.zcore.utils;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.menu.hooks.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Creates and manages a smooth BossBar animation for a group of players.
 * Uses the Folia-aware scheduler to handle the timing and updates of the BossBar.
 */
public class BarAnimation {

    private final BossBar bossBar;
    private final double totalTime;
    private double remainingTime;
    private WrappedTask task;

    /**
     * Creates a new smooth BossBar animation for a group of players.
     *
     * @param players  the list of players to display the BossBar to.
     * @param text     the text to display on the BossBar.
     * @param seconds  the total duration of the animation in seconds.
     * @param barColor the color of the BossBar.
     * @param barStyle the style of the BossBar.
     */
    public BarAnimation(List<Player> players, String text, int seconds, BarColor barColor, BarStyle barStyle) {
        this.bossBar = Bukkit.createBossBar(text, barColor, barStyle);
        this.totalTime = seconds * 20.0; // Convert seconds to ticks (20 ticks = 1 second)
        this.remainingTime = totalTime;

        for (Player player : players) {
            this.bossBar.addPlayer(player);
        }

        this.bossBar.setVisible(true);
        // Schedule the task to run every tick (1L) for the smoothest animation
        JobsPlugin plugin = JavaPlugin.getPlugin(JobsPlugin.class);
        this.task = plugin.getScheduler().runTimer(this::run, 0L, 1L);
    }

    /**
     * The task to run on each tick. Updates the BossBar's progress.
     * Cancels the task and removes the BossBar when the time runs out.
     */
    private void run() {
        double progress = remainingTime / totalTime;
        bossBar.setProgress(progress);

        if (remainingTime <= 0) {
            bossBar.removeAll();
            if (this.task != null) {
                this.task.cancel(); // Stop the task when the BossBar is empty
                this.task = null;
            }
        }

        remainingTime -= 1; // Decrease by 1 tick at each update
    }
}