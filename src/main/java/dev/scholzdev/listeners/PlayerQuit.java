package dev.scholzdev.listeners;

import dev.scholzdev.Spectator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuit implements Listener {

    private final Spectator plugin;

    public PlayerQuit(Spectator plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        Player spectator = plugin.getSpectateManager().getCurrentSpectator();

        if (spectator == null) return;

        if (spectator.equals(quitter)) {
            plugin.getSpectateManager().stopSpectating();
            return;
        }

        if (spectator.getSpectatorTarget() == quitter) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (spectator.isOnline()) {
                    spectator.setSpectatorTarget(null);
                    plugin.getSpectateManager().switchToNextPlayer();
                }
            }, 1L);
        }
    }
}
