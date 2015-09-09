package me.exerosis.bungee;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandManager implements CommandExecutor {
    private BungeeChatClient client;

    public CommandManager(BungeeChatClient client) {
        client.getCommand("bungeeChat").setExecutor(this);
        this.client = client;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (command.getLabel().equalsIgnoreCase("bungeeChat") && strings.length > 0)
            if (strings[0].equalsIgnoreCase("enable")) {
                if (client.isBCEnabled()) {
                    commandSender.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "BungeeChat is already forwarding this server's chat!");
                    return true;
                }
                client.setBCEnabled(false, true);
                commandSender.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "BungeeChat has started forwarding this server's chat!");
                return true;
            }
        if (strings[0].equalsIgnoreCase("disable")) {
            if (!client.isBCEnabled()) {
                commandSender.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "BungeeChat is not forwarding this server's chat!");
                return true;
            }
            client.setBCEnabled(false, true);
            commandSender.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "BungeeChat has stopped forwarding this server's chat!");
            return true;
        }
        return false;
    }
}
