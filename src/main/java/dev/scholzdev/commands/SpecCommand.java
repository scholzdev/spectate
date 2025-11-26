package dev.scholzdev.commands;

import dev.scholzdev.Spectator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpecCommand implements CommandExecutor, TabCompleter {

    private final Spectator plugin;

    public SpecCommand(Spectator plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        // Set Subcommand
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("spectator.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to change settings.");
                return true;
            }

            return handleSetCommand(player, args);
        }

        // Reload Subcommand
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("spectator.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to reload the config.");
                return true;
            }

            plugin.reloadConfig();
            plugin.loadConfigValues();
            player.sendMessage(ChatColor.GREEN + "Config reloaded successfully!");
            displayCurrentSettings(player);
            return true;
        }

        // Info Subcommand
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            displayCurrentSettings(player);
            return true;
        }

        // Next Subcommand
        if (args.length > 0 && args[0].equalsIgnoreCase("next")) {
            if (!plugin.getSpectateManager().isSpectating(player)) {
                player.sendMessage(ChatColor.RED + "You are not spectating!");
                return true;
            }

            plugin.getSpectateManager().switchToNextPlayer();
            return true;
        }

        // Permission Check
        if (!player.hasPermission("spectator.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Force Spectator Check
        if (plugin.isForceSpectatorEnabled() && !player.getName().equals(plugin.getForcedSpectatorName())) {
            player.sendMessage(ChatColor.RED + "Only " + plugin.getForcedSpectatorName() + " is allowed to use this command.");
            return true;
        }

        // Toggle Spectate
        if (args.length == 0) {
            if (plugin.getSpectateManager().isSpectating(player)) {
                plugin.getSpectateManager().stopSpectating();
                player.sendMessage(ChatColor.RED + "You stopped spectating.");
            } else {
                plugin.getSpectateManager().startSpectating(player);
                player.sendMessage(ChatColor.GREEN + "You are now spectating. Cycle started!");
            }
            return true;
        }

        sendHelpMessage(player);
        return true;
    }

    private boolean handleSetCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage:");
            player.sendMessage(ChatColor.YELLOW + "/spec set force enable <true/false>");
            player.sendMessage(ChatColor.YELLOW + "/spec set force name <name>");
            player.sendMessage(ChatColor.YELLOW + "/spec set twitch delay <seconds>");
            player.sendMessage(ChatColor.YELLOW + "/spec set twitch command <command>");
            player.sendMessage(ChatColor.YELLOW + "/spec set cycle delay <seconds>");
            player.sendMessage(ChatColor.YELLOW + "/spec set channel <name>");
            return true;
        }

        String category = args[1].toLowerCase();
        String setting = args[2].toLowerCase();

        switch (category) {
            case "force" -> {
                return handleForceSettings(player, setting, args);
            }
            case "twitch" -> {
                return handleTwitchSettings(player, setting, args);
            }
            case "cycle" -> {
                if (setting.equals("delay") && args.length >= 4) {
                    try {
                        int delay = Integer.parseInt(args[3]);
                        if (delay < 1) {
                            player.sendMessage(ChatColor.RED + "Delay must be at least 1 second!");
                            return true;
                        }
                        plugin.getConfig().set("cycle-delay-seconds", delay);
                        plugin.saveConfig();
                        plugin.loadConfigValues();
                        player.sendMessage(ChatColor.GREEN + "Cycle delay set to " + delay + " seconds!");
                        return true;
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
                        return true;
                    }
                }
            }
            case "channel" -> {
                if (setting.equals("name") && args.length >= 4) {
                    String channelName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    plugin.getConfig().set("channel", channelName);
                    plugin.saveConfig();
                    plugin.loadConfigValues();
                    player.sendMessage(ChatColor.GREEN + "Channel set to: " + channelName);
                    return true;
                }
            }
        }

        player.sendMessage(ChatColor.RED + "Unknown setting!");
        return true;
    }

    private boolean handleForceSettings(Player player, String setting, String[] args) {
        if (setting.equals("enable") && args.length >= 4) {
            boolean enable = Boolean.parseBoolean(args[3]);
            plugin.getConfig().set("force-spectator", enable);
            plugin.saveConfig();
            plugin.loadConfigValues();
            player.sendMessage(ChatColor.GREEN + "Force spectator " + (enable ? "enabled" : "disabled") + "!");
            return true;
        } else if (setting.equals("name") && args.length >= 4) {
            String name = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            plugin.getConfig().set("forced-spectator-name", name);
            plugin.saveConfig();
            plugin.loadConfigValues();
            player.sendMessage(ChatColor.GREEN + "Forced spectator name set to: " + name);
            return true;
        }
        return false;
    }

    private boolean handleTwitchSettings(Player player, String setting, String[] args) {
        if (setting.equals("delay") && args.length >= 4) {
            try {
                int delay = Integer.parseInt(args[3]);
                if (delay < 1) {
                    player.sendMessage(ChatColor.RED + "Delay must be at least 1 second!");
                    return true;
                }
                plugin.getConfig().set("twitch-command-delay-seconds", delay);
                plugin.saveConfig();
                plugin.loadConfigValues();
                player.sendMessage(ChatColor.GREEN + "Twitch command delay set to " + delay + " seconds!");
                return true;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
                return true;
            }
        } else if (setting.equals("command") && args.length >= 4) {
            String cmd = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            plugin.getConfig().set("twitch-command", cmd);
            plugin.saveConfig();
            plugin.loadConfigValues();
            player.sendMessage(ChatColor.GREEN + "Twitch command set to: " + cmd);
            return true;
        }
        return false;
    }

    private void displayCurrentSettings(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Current Settings ===");
        player.sendMessage(ChatColor.YELLOW + "Cycle Delay: " + ChatColor.WHITE + plugin.getCycleDelaySeconds() + "s");
        player.sendMessage(ChatColor.YELLOW + "Force Spectator: " + ChatColor.WHITE + plugin.isForceSpectatorEnabled());
        if (plugin.isForceSpectatorEnabled()) {
            player.sendMessage(ChatColor.YELLOW + "Forced Name: " + ChatColor.WHITE + plugin.getForcedSpectatorName());
        }
        player.sendMessage(ChatColor.YELLOW + "Twitch Delay: " + ChatColor.WHITE + plugin.getTwitchCommandDelaySeconds() + "s");
        player.sendMessage(ChatColor.YELLOW + "Twitch Command: " + ChatColor.WHITE + plugin.getTwitchCommand());
        player.sendMessage(ChatColor.YELLOW + "Channel: " + ChatColor.WHITE + plugin.getChannel());
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Spectator Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/spec" + ChatColor.GRAY + " - Toggle spectating");
        player.sendMessage(ChatColor.YELLOW + "/spec next" + ChatColor.GRAY + " - Switch to next player");
        player.sendMessage(ChatColor.YELLOW + "/spec info" + ChatColor.GRAY + " - Show current settings");
        if (player.hasPermission("spectator.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/spec reload" + ChatColor.GRAY + " - Reload config");
            player.sendMessage(ChatColor.YELLOW + "/spec set" + ChatColor.GRAY + " - Change settings");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("next", "info"));
            if (sender.hasPermission("spectator.admin")) {
                completions.addAll(Arrays.asList("set", "reload"));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            completions.addAll(Arrays.asList("force", "twitch", "cycle", "channel"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            switch (args[1].toLowerCase()) {
                case "force" -> completions.addAll(Arrays.asList("enable", "name"));
                case "twitch" -> completions.addAll(Arrays.asList("delay", "command"));
                case "cycle" -> completions.add("delay");
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            if (args[1].equalsIgnoreCase("force") && args[2].equalsIgnoreCase("enable")) {
                completions.addAll(Arrays.asList("true", "false"));
            }
        }

        return completions;
    }
}