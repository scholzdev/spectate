package dev.scholzdev.twitch;

import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import dev.scholzdev.Spectator;
import dev.scholzdev.manager.SpectateManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TwitchEventHandler {

    private Spectator plugin;
    private SpectateManager spectateManager;
    private long lastCommandTime = 0L;
    private final long twitchCommandDelay;
    private final String twitchCommand;

    public TwitchEventHandler(Spectator spectator, SpectateManager spectateManager, long twitchCommandDelaySeconds) {
        this.plugin = spectator;
        this.spectateManager = spectateManager;
        this.twitchCommandDelay = twitchCommandDelaySeconds * 1000L;
        this.twitchCommand = plugin.getConfig().getString("twitch-command", "!nextplayer");
    }

    @EventSubscriber
    public void onMessage(ChannelMessageEvent event) {
        String message = event.getMessage().trim().toLowerCase();

        if (message.equals(this.twitchCommand)) {
            broadcast(ChatColor.DARK_GRAY + "[Twitch] " + event.getUser().getName() + ": " + message);
            handleNextPlayerCommand(event);
        }
    }

    private void handleNextPlayerCommand(ChannelMessageEvent event) {
        String user = event.getUser().getName();

        long now = System.currentTimeMillis();
        long elapsed = now - lastCommandTime;

        if (elapsed < twitchCommandDelay) {
            long secondsLeft = (twitchCommandDelay - elapsed) / 1000;
            broadcast(ChatColor.RED + "Twitch command on cooldown (" + secondsLeft + "s left)");
            return;
        }

        lastCommandTime = now;

        Player spectator = spectateManager.getCurrentSpectator();

        if (spectator == null) {
            broadcast(ChatColor.RED + "No active spectator - cannot switch player!");
            return;
        }

        spectateManager.switchToNextPlayer();
        broadcast(ChatColor.GREEN + user + " forced next player!");
    }

    private void broadcast(String message) {
        this.plugin.getServer().broadcastMessage(message);
    }

}
