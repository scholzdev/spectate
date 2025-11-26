package dev.scholzdev;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import dev.scholzdev.commands.SpecCommand;
import dev.scholzdev.listeners.PlayerDeath;
import dev.scholzdev.listeners.PlayerQuit;
import dev.scholzdev.manager.SpectateManager;
import dev.scholzdev.twitch.TwitchEventHandler;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Spectator extends JavaPlugin {

    private SpectateManager spectateManager;
    private ITwitchClient client;

    // Config Values
    private int cycleDelaySeconds;
    private int twitchCommandDelaySeconds;
    private String twitchCommand;
    private boolean forceSpectatorEnabled;
    private String forcedSpectatorName;
    private String channel;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        spectateManager = new SpectateManager(this, cycleDelaySeconds * 20);
        getServer().getPluginCommand("spectate").setExecutor(new SpecCommand(this));

        setupTwitchIntegration(spectateManager);

        getServer().getPluginManager().registerEvents(new PlayerQuit(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeath(this), this);
    }

    @Override
    public void onDisable() {
        if (spectateManager != null && spectateManager.getCurrentSpectator() != null) {
            spectateManager.stopSpectating();
        }
        getLogger().info("Spectator Plugin disabled!");
    }

    private void setupTwitchIntegration(SpectateManager spectateManager) {
        client = TwitchClientBuilder.builder()
                .withEnableChat(true)
                .withEnableHelix(false)
                .withChatCommandsViaHelix(false)
                .build();

        final String channel = getConfig().getString("channel", null);

        if(channel != null) {
            client.getChat().joinChannel(channel);
        }

        final long twitchCommandDelaySeconds = getConfig().getLong("twitch-command-delay-seconds", 10);

        client.getEventManager().getEventHandler(SimpleEventHandler.class).registerListener(new TwitchEventHandler(this, spectateManager, twitchCommandDelaySeconds));
    }

    public void loadConfigValues() {
        cycleDelaySeconds = getConfig().getInt("cycle-delay-seconds", 10);
        twitchCommandDelaySeconds = getConfig().getInt("twitch-command-delay-seconds", 10);
        twitchCommand = getConfig().getString("twitch-command", "!kekw");
        forceSpectatorEnabled = getConfig().getBoolean("force-spectator", false);
        forcedSpectatorName = getConfig().getString("forced-spectator-name", "example_name");
        channel = getConfig().getString("channel", "example_name");

        if (spectateManager != null) {
            spectateManager.setCycleDelay(cycleDelaySeconds * 20);
        }

        getLogger().info("Config values loaded/reloaded!");
    }
}