package de.muv1n.whitelist;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class WhitelistCommand extends Command {

    private final Whitelist plugin;

    public WhitelistCommand(Whitelist plugin) {
        super("muvwl");
        this.plugin = plugin;
        setDescription("MuvWhitelist management");
        setUsage("/muvwl <reload|status|sync>");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.startSync();
                sender.sendMessage("§aMuvWhitelist config reloaded and sync restarted.");
            }
            case "status" -> {
                int count = plugin.getServer().getWhitelistedPlayers().size();
                sender.sendMessage("§aMuvWhitelist running. §7Whitelisted players: §f" + count);
            }
            case "sync" -> {
                sender.sendMessage("§7Triggering manual sync...");
                new WhitelistSyncTask(plugin).runTaskAsynchronously(plugin);
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6/muvwl reload §7— reload config and restart sync timer");
        sender.sendMessage("§6/muvwl status §7— show current whitelisted player count");
        sender.sendMessage("§6/muvwl sync §7— trigger an immediate sync now");
    }
}
