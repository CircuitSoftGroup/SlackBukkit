package us.circuitsoft.slack.bukkit;

import java.text.MessageFormat;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SlackBukkit extends JavaPlugin implements Listener {

    private static String webhookUrl;
    private List<String> blacklist;

    @Override
    public void onEnable() {
        getLogger().info("Slack has been enabled.");
        getServer().getPluginManager().registerEvents(this, this);
        updateConfig(getDescription().getVersion());
        webhookUrl = getConfig().getString("webhook");
        blacklist = getConfig().getStringList("blacklist");
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || webhookUrl.equals("https://hooks.slack.com/services/")) {
            getLogger().severe("You have not set your webhook URL in the config!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Slack has been disabled!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        if (isVisible("slack.hide.chat", event.getPlayer())) {
            send('"' + event.getMessage() + '"', event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerJoinEvent event) {
        if (isVisible("slack.hide.login", event.getPlayer())) {
            send("logged in", event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (isVisible("slack.hide.logout", event.getPlayer())) {
            send("logged out", event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (isAllowed(event.getMessage()) && isVisible("slack.hide.command", event.getPlayer()) && !event.getMessage().contains("/slack send")) {
            send(event.getMessage(), event.getPlayer().getName());
        }
    }

    public void send(String message, String name) {
        new SlackBukkitPoster(this, message, name, null).runTaskAsynchronously(this);
    }

    public void send(String message, String name, String iconUrl) {
        new SlackBukkitPoster(this, message, name, iconUrl).runTaskAsynchronously(this);
    }

    private boolean isAllowed(String command) {
        if (getConfig().getBoolean("use-blacklist")) {
            return !blacklist.contains(command);
        } else {
            return true;
        }
    }

    private void updateConfig(String version) {
        this.saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        getConfig().set("version", version);
        saveConfig();
    }

    private boolean isVisible(String permission, Player player) {
        if (getConfig().getBoolean("use-perms")) {
            return !player.hasPermission(permission);
        } else {
            return true;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("slack")) {
            return false;
        }
        if (!sender.hasPermission("slack.command")) {
            sender.sendMessage(ChatColor.DARK_RED + "You are not allowed to execute this command!");
        } else if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "/slack send <username> <image URL or null for username's skin> <message> - send a custom message to slack\n/slack reload - reload Slack's config");
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("slack.command")) {
                reloadConfig();
                webhookUrl = getConfig().getString("webhook");
                blacklist = getConfig().getStringList("blacklist");
                sender.sendMessage(ChatColor.GREEN + "Slack has been reloaded.");
                if (!(sender instanceof ConsoleCommandSender)) {
                    getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Slack has been reloaded by " + sender.getName() + '.');
                }
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "You are not allowed to execute this command!");
            }
        } else if (args[0].equalsIgnoreCase("send")) {
            if (sender.hasPermission("slack.command")) {
                if (args.length <= 3) {
                    sender.sendMessage(ChatColor.GOLD + "/slack send <username> <image URL or null for username's skin> <message>");
                } else if (args.length >= 4) {
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (int i = 3; i < args.length; i++) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ");
                        }
                        sb.append(args[i]);
                    }
                    String senderName;
                    if (sender instanceof ConsoleCommandSender) {
                        senderName = "Console";
                    } else {
                        senderName = sender.getName();
                    }
                    sb.append(MessageFormat.format(" (sent by {0})", senderName));
                    send(sb.toString(), args[1], args[2].equalsIgnoreCase("null") ? null : args[2]);
                }
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "You are not allowed to execute this command!");
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + "/slack send <username> <image URL or null for username's skin> <message> - send a custom message to slack \n/slack reload - reload Slack's config");
        }
        return true;
    }

    public static String getWebhookUrl() {
        return webhookUrl;
    }
}
