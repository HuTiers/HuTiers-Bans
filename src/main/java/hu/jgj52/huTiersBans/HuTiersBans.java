package hu.jgj52.huTiersBans;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import hu.jgj52.huTiersBans.Commands.BanCommand;
import hu.jgj52.huTiersBans.Commands.KickCommand;
import hu.jgj52.huTiersBans.Commands.UnBanCommand;
import hu.jgj52.huTiersBans.Utils.Reason;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static hu.jgj52.databaseVelocity.DatabaseVelocity.postgres;

@Plugin(id = "hutiers-bans", name = "HuTiers-Bans", version = "1.7", authors = {"JGJ52", "Polokalap"}, dependencies = {@Dependency(id = "hutiers-messenger_velocity")})
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
        server.getCommandManager().register(server.getCommandManager().metaBuilder("kick").build(), new KickCommand(this));
    }

    @Subscribe
    public void beforeJoin(LoginEvent event) {
        postgres.from("bans").eq("uuid", event.getPlayer().getUniqueId()).execute().thenAccept(result -> {
            if (!result.isEmpty()) {
                if (Long.parseLong(result.first().get("expires").toString()) <= System.currentTimeMillis()) {
                    postgres.from("bans").eq("expires", result.first().get("expires")).delete();
                    return;
                }
                event.getPlayer().disconnect(Reason.banReason(result));
            } else {
                postgres.from("bans").eq("name", event.getPlayer().getUsername()).execute().thenAccept(res -> {
                    if (Long.parseLong(res.first().get("expires").toString()) <= System.currentTimeMillis()) {
                        postgres.from("bans").eq("expires", res.first().get("expires")).delete();
                        return;
                    }
                    event.getPlayer().disconnect(Reason.banReason(res));
                    postgres.from("bans").eq("expires", res.first().get("expires")).update(Map.of("uuid", event.getPlayer().getUniqueId()));
                });
            }
        });
    }
}
