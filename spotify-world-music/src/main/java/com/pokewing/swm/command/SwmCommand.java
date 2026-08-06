package com.pokewing.swm.command;

import com.pokewing.swm.SpotifyWorldMusic;
import com.pokewing.swm.config.MusicZone;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** {@code /swm} - player facing helpers plus the admin commands. */
public final class SwmCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "swm.admin";

    private final SpotifyWorldMusic plugin;

    public SwmCommand(SpotifyWorldMusic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "player" -> playerCommand(sender);
            case "link" -> linkCommand(sender);
            case "unlink" -> unlinkCommand(sender);
            case "status" -> statusCommand(sender);
            case "resync" -> resyncCommand(sender);
            case "zones" -> zonesCommand(sender);
            case "reload" -> reloadCommand(sender);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        plugin.messages().raw(sender, "&8&m                                        ");
        plugin.messages().raw(sender, " &a&lSpotifyWorldMusic");
        plugin.messages().raw(sender, " &e/swm player &7- tarayici oynatici adresini al");
        plugin.messages().raw(sender, " &e/swm link &7- Spotify hesabini bagla");
        plugin.messages().raw(sender, " &e/swm unlink &7- Spotify baglantisini kaldir");
        plugin.messages().raw(sender, " &e/swm status &7- durumunu goster");
        plugin.messages().raw(sender, " &e/swm resync &7- muzigi yeniden baslat");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.messages().raw(sender, " &e/swm zones &7- tanimli bolgeleri listele");
            plugin.messages().raw(sender, " &e/swm reload &7- config.yml yeniden yukle");
        }
        plugin.messages().raw(sender, "&8&m                                        ");
    }

    private void playerCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "&cBu komut sadece oyunda calisir.");
            return;
        }
        if (!plugin.config().webUsable()) {
            plugin.messages().send(sender, "&cTarayici oynatici bu sunucuda kapali.");
            return;
        }
        plugin.messages().sendLink(player,
                "&7Sana ozel oynatici adresi: &b[TIKLA VE AC]", plugin.webPlayerUrl(player));
        plugin.messages().send(player,
                "&8Adresi kimseyle paylasma, senin hesabina bagli.");
    }

    private void linkCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "&cBu komut sadece oyunda calisir.");
            return;
        }
        if (plugin.auth() == null) {
            plugin.messages().send(sender,
                    "&cSpotify API modu ayarli degil. Sunucu sahibi config.yml icindeki "
                            + "spotify.client-id / client-secret / redirect-uri alanlarini doldurmali.");
            return;
        }
        String url = plugin.auth().beginLink(player.getUniqueId(), player.getName());
        plugin.messages().sendLink(player,
                "&7Spotify hesabini baglamak icin &b[TIKLA] &7(10 dakika gecerli)", url);
        plugin.messages().send(player,
                "&8Not: Spotify'in baska cihazi kontrol etmesi icin Premium hesap gerekir.");
    }

    private void unlinkCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "&cBu komut sadece oyunda calisir.");
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!plugin.links().isLinked(uuid)) {
            plugin.messages().send(sender, "&7Zaten bagli bir Spotify hesabin yok.");
            return;
        }
        plugin.links().unlink(uuid);
        plugin.messages().send(sender, "&aSpotify baglantin kaldirildi.");
    }

    private void statusCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "&7Mod: &f" + plugin.config().mode()
                    + " &7| bolge: &f" + plugin.config().zones().size()
                    + " &7| bagli hesap: &f" + plugin.links().linkedCount());
            return;
        }
        MusicZone zone = plugin.music().zoneOf(player.getUniqueId());
        plugin.messages().send(player, "&7Bolge: &f"
                + (zone == null ? "yok" : zone.displayName()));
        if (zone != null) {
            plugin.messages().send(player, "&7Parca: &f" + zone.rawUrl()
                    + " &7(dongu: " + (zone.loop() ? "&aacik" : "&ckapali") + "&7)");
        }
        boolean linked = plugin.links().isLinked(player.getUniqueId());
        String spotifyName = plugin.links().spotifyName(player.getUniqueId());
        plugin.messages().send(player, "&7Spotify hesabi: "
                + (linked ? "&abagli" + (spotifyName == null ? "" : " &7(" + spotifyName + ")") : "&cbagli degil"));
        if (plugin.config().webUsable()) {
            plugin.messages().sendLink(player, "&7Tarayici oynatici: &b[AC]",
                    plugin.webPlayerUrl(player));
        }
    }

    private void resyncCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "&cBu komut sadece oyunda calisir.");
            return;
        }
        plugin.music().update(player, true);
        plugin.messages().send(player, "&aMuzik yeniden baslatildi.");
    }

    private void zonesCommand(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.messages().send(sender, "&cBu komut icin yetkin yok.");
            return;
        }
        List<MusicZone> zones = plugin.config().zones();
        if (zones.isEmpty()) {
            plugin.messages().send(sender, "&7Tanimli bolge yok.");
            return;
        }
        plugin.messages().send(sender, "&7Tanimli bolgeler (&f" + zones.size() + "&7):");
        for (MusicZone zone : zones) {
            String scope = zone.regionEnabled()
                    ? zone.worldName() + " &8(r=" + (int) zone.radius() + ")"
                    : zone.worldName();
            plugin.messages().raw(sender, " &8- &a" + zone.id() + " &7-> &f" + scope
                    + " &7| " + zone.contextType()
                    + " | dongu: " + (zone.loop() ? "&aacik" : "&ckapali")
                    + " &7| oncelik: " + zone.priority());
        }
    }

    private void reloadCommand(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.messages().send(sender, "&cBu komut icin yetkin yok.");
            return;
        }
        try {
            plugin.reloadEverything(false);
            plugin.messages().send(sender, "&aconfig.yml yeniden yuklendi. &7Bolge sayisi: &f"
                    + plugin.config().zones().size());
        } catch (RuntimeException ex) {
            plugin.messages().send(sender, "&cYeniden yukleme basarisiz: " + ex.getMessage());
            plugin.getLogger().warning("Reload failed: " + ex);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            List<String> all = new ArrayList<>(List.of("player", "link", "unlink", "status", "resync"));
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                all.add("zones");
                all.add("reload");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String option : all) {
                if (option.startsWith(prefix)) {
                    options.add(option);
                }
            }
        }
        return options;
    }

    /** Convenience used by tests / other plugins that want to force a refresh. */
    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.music().update(player, true);
        }
    }
}
