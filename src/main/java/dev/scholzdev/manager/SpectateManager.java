package dev.scholzdev.manager;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class SpectateManager {

    private final JavaPlugin plugin;

    @Getter
    @Setter
    private int cycleDelay;

    @Getter
    private Player currentSpectator;
    private SpectatorData spectatorData;
    private BukkitRunnable cycleTask;

    public SpectateManager(JavaPlugin plugin, int cycleDelay) {
        this.plugin = plugin;
        this.cycleDelay = cycleDelay;
    }

    public void startSpectating(Player player) {
        if (currentSpectator != null) {
            stopSpectating();
        }

        this.currentSpectator = player;
        this.spectatorData = new SpectatorData(
                player.getGameMode(),
                player.getLocation().clone(),
                player.getAllowFlight(),
                player.isFlying()
        );

        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(ChatColor.GREEN + "You are now spectating.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.equals(currentSpectator)) {
                startCycling(player);
            }
        }, 10L);
    }

    public void stopSpectating() {
        if (currentSpectator == null) return;

        if (cycleTask != null) {
            cycleTask.cancel();
            cycleTask = null;
        }

        Player player = currentSpectator;
        SpectatorData data = spectatorData;

        currentSpectator = null;
        spectatorData = null;

        player.setSpectatorTarget(null);

        if (data != null) {
            player.setGameMode(data.gameMode);
            player.teleport(data.location);
            player.setAllowFlight(data.allowFlight);
            player.setFlying(data.flying);
        }

        player.sendMessage(ChatColor.YELLOW + "Stopped spectating.");
    }

    private void startCycling(Player spectator) {
        cycleTask = new BukkitRunnable() {
            private int currentIndex = 0;

            @Override
            public void run() {
                if (currentSpectator == null || !spectator.isOnline()) {
                    cancel();
                    return;
                }

                List<Player> players = getAvailablePlayers(spectator);

                if (players.isEmpty()) {
                    spectator.sendMessage(ChatColor.RED + "No players to spectate.");
                    spectator.setSpectatorTarget(null);
                    return;
                }

                if (currentIndex >= players.size()) {
                    currentIndex = 0;
                }

                Player target = players.get(currentIndex);
                currentIndex++;

                teleportAndSpectate(spectator, target);
            }
        };

        cycleTask.runTaskTimer(plugin, 0L, cycleDelay);
    }

    private void teleportAndSpectate(Player spectator, Player target) {
        if (!spectator.isOnline() || !target.isOnline()) return;

        Location targetLoc = target.getLocation().clone();

        spectator.getWorld().getChunkAtAsync(targetLoc).thenAccept(chunk -> {
            spectator.teleportAsync(targetLoc).thenRun(() -> {

                Bukkit.getScheduler().runTaskLater(plugin, () -> {

                    if (!spectator.isOnline() || !target.isOnline()) return;

                    if (!spectator.getWorld().equals(target.getWorld())) return;
                    if (spectator.getLocation().distanceSquared(target.getLocation()) > 10000) return;

                    spectator.setSpectatorTarget(target);
                    spectator.sendMessage(ChatColor.GREEN + "Now spectating " + target.getName());

                }, 10L);
            });
        });
    }

    public void switchToNextPlayer() {
        if (currentSpectator == null) return;

        List<Player> players = getAvailablePlayers(currentSpectator);
        if (players.isEmpty()) {
            currentSpectator.sendMessage(ChatColor.RED + "No players to spectate.");
            currentSpectator.setSpectatorTarget(null);
            return;
        }

        Player nextTarget = players.get(0);
        teleportAndSpectate(currentSpectator, nextTarget);
    }

    private List<Player> getAvailablePlayers(Player spectator) {
        List<Player> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(spectator)
                    && p.getGameMode() != GameMode.SPECTATOR
                    && p.isOnline()) {
                players.add(p);
            }
        }
        return players;
    }

    public boolean isSpectating(Player player) {
        return currentSpectator != null && currentSpectator.equals(player);
    }

    public static class SpectatorData {
        final GameMode gameMode;
        final Location location;
        final boolean allowFlight;
        final boolean flying;

        public SpectatorData(GameMode gameMode, Location location, boolean allowFlight, boolean flying) {
            this.gameMode = gameMode;
            this.location = location;
            this.allowFlight = allowFlight;
            this.flying = flying;
        }
    }
}
