package hu.jgj52.huTiersBans;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import hu.jgj52.databaseVelocity.PostgreSQL;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static hu.jgj52.databaseVelocity.DatabaseVelocity.postgres;

@Plugin(id = "hutiers-bans", name = "HuTiers-Bans", version = "1.0", authors = {"JGJ52"})
public class HuTiersBans {

    public final ProxyServer server;
    private final Logger logger;

    @Inject
    public HuTiersBans(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getCommandManager().register(server.getCommandManager().metaBuilder("ban").build(), new BanCommand(this));
        server.getCommandManager().register(server.getCommandManager().metaBuilder("unban").build(), new UnBanCommand(this));
    }

    @Subscribe
    public void beforeJoin(PreLoginEvent event) {
        try {
            PostgreSQL.QueryResult result = postgres.from("bans").eq("uuid", event.getUniqueId()).execute().get();
            if (!result.isEmpty()) {
                Map<String, Object> ban = result.first();
                SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("Europe/Budapest"));

                StringBuilder sb = new StringBuilder();
                long diff = Long.parseLong(ban.get("expires").toString()) - System.currentTimeMillis();

                if (diff > 0) {
                    long seconds = diff / 1000;
                    long days = seconds / 86400;
                    seconds %= 86400;
                    long hours = seconds / 3600;
                    seconds %= 3600;
                    long minutes = seconds / 60;
                    seconds %= 60;
                    if (days > 0) sb.append(days).append(" nap ");
                    if (hours > 0) sb.append(hours).append(" óra ");
                    if (minutes > 0) sb.append(minutes).append(" perc ");
                    if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append(" másodperc");
                } else {
                    postgres.from("bans").eq("uuid", event.getUniqueId()).delete().get();
                    return;
                }

                String expires = df.format(new Date(Long.parseLong(ban.get("expires").toString())));
                String in = sb.toString().trim();

                String reason = ban.get("reason").toString();

                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text(
                        "§cKi vagy tiltva a szerverről:\n" +
                                "§7Oka: §f" + reason.replaceAll("&", "§") + "\n" +
                                "§7Lejár: §6" + expires + "\n" +
                                "§7azaz §6" + in + " múlva"
                )));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
