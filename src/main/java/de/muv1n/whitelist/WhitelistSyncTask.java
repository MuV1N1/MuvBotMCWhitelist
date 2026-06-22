package de.muv1n.whitelist;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WhitelistSyncTask extends BukkitRunnable {

    private final Whitelist plugin;

    public WhitelistSyncTask(Whitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        String apiKey = plugin.getConfig().getString("api-key", "");
        boolean removeUnlisted = plugin.getConfig().getBoolean("remove-unlisted", false);

        if (apiKey.isBlank() || apiKey.equals("paste-your-uuid-here")) {
            plugin.getLogger().warning("No API key configured — edit config.yml and run /muvwl reload");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BuildConstants.BACKEND_URL + "/api/mc/whitelist"))
                    .header("X-Api-Key", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                plugin.getLogger().warning("Invalid API key — check config.yml");
                return;
            }
            if (response.statusCode() != 200) {
                plugin.getLogger().info(response.body());
                plugin.getLogger().warning("Backend returned " + response.statusCode() + ", skipping sync");
                return;
            }

            List<String> incoming = parseJsonArray(response.body());

            // setWhitelisted() fires a Bukkit event — must run on the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                syncWhitelist(incoming, removeUnlisted);
                plugin.getLogger().info("Whitelist synced — " + incoming.size() + " players");
            });

        } catch (Exception e) {
            // Never touch the whitelist on error — just skip this cycle
            plugin.getLogger().warning("Sync failed, keeping existing whitelist: " + e.getMessage());
        }
    }

    private void syncWhitelist(List<String> names, boolean removeUnlisted) {
        Server server = plugin.getServer();

        Set<String> current = server.getWhitelistedPlayers().stream()
                .map(OfflinePlayer::getName)
                .collect(Collectors.toSet());

        for (String name : names) {
            if (!current.contains(name)) {
                server.getOfflinePlayer(name).setWhitelisted(true);
                plugin.getLogger().info("Added to whitelist: " + name);
            }
        }

        if (removeUnlisted) {
            Set<String> incomingSet = new HashSet<>(names);
            for (OfflinePlayer p : server.getWhitelistedPlayers()) {
                if (p.getName() != null && !incomingSet.contains(p.getName())) {
                    p.setWhitelisted(false);
                    plugin.getLogger().info("Removed from whitelist: " + p.getName());
                }
            }
        }
    }

    private List<String> parseJsonArray(String json) {
        return Arrays.stream(json.replaceAll("[\\[\\]\"\\s]", "").split(","))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
