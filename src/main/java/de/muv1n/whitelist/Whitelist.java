package de.muv1n.whitelist;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Whitelist extends JavaPlugin {

    private BukkitTask syncTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startSync();
        Bukkit.getCommandMap().register("muvwl", "muvwhitelist", new WhitelistCommand(this));
        getLogger().info("MuvWhitelist enabled.");
    }

    @Override
    public void onDisable() {
        if (syncTask != null) syncTask.cancel();
        getLogger().info("MuvWhitelist disabled.");
    }

    public void startSync() {
        if (syncTask != null) syncTask.cancel();
        long intervalSeconds = getConfig().getLong("sync-interval-seconds", 300);
        syncTask = new WhitelistSyncTask(this)
                .runTaskTimerAsynchronously(this, 0L, intervalSeconds * 20L);
        getLogger().info("Sync scheduled every " + intervalSeconds + "s.");
    }
}
