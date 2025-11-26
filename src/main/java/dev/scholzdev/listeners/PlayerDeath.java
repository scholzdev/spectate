package dev.scholzdev.listeners;

import dev.scholzdev.Spectator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {

    private final Spectator plugin;

    public PlayerDeath(Spectator plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player died = event.getEntity();

        // find the current spectator (only one)
        Player spectator = plugin.getSpectateManager().getCurrentSpectator();
        if (spectator == null) return;

        if (spectator.getSpectatorTarget() == died) {
            // Delay the switch to allow Minecraft to reset the camera
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (spectator.isOnline()) {
                    spectator.setSpectatorTarget(null);
                    plugin.getSpectateManager().switchToNextPlayer();
                }
            }, 1L);
        }
    }
}
