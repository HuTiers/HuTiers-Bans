package hu.jgj52.huTiersBans;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import hu.jgj52.databaseVelocity.PostgreSQL;
import hu.jgj52.huTiersBans.Commands.BanCommand;
import hu.jgj52.huTiersBans.Commands.KickCommand;
import hu.jgj52.huTiersBans.Commands.UnBanCommand;
import hu.jgj52.huTiersBans.Utils.Reason;
import hu.jgj52.huTiersMessengerVelocity.Messenger;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

import static hu.jgj52.databaseVelocity.DatabaseVelocity.postgres;

@Plugin(id = "hutiers-bans", name = "HuTiers-Bans", version = "1.5", authors = {"JGJ52", "Polokalap"}, dependencies = {@Dependency(id = "hutiers-messenger_velocity")})
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

        Messenger.listen("ban", message -> new Thread(() -> {
            String[] args = message.split(" ", 5);
            String uuid = args[0];
            String name = args[1];
            String by = args[2];
            String time = args[3];
            String reason = args[4];

            try {
                PostgreSQL.QueryResult result = postgres.from("bans").eq("uuid", UUID.fromString(uuid)).execute().get();
                if (!result.isEmpty()) return;
            } catch (Exception e) {
                e.printStackTrace();
            }

            server.getScheduler().buildTask(this, () -> {
                long exp = System.currentTimeMillis();

                if (time.endsWith("s")) {
                    exp += Long.parseLong(time.replaceAll("s", "")) * 1000;
                } else if (time.endsWith("m")) {
                    exp += Long.parseLong(time.replaceAll("m", "")) * 1000 * 60;
                } else if (time.endsWith("h")) {
                    exp += Long.parseLong(time.replaceAll("h", "")) * 1000 * 60 * 60;
                } else if (time.endsWith("d")) {
                    exp += Long.parseLong(time.replaceAll("d", "")) * 1000 * 60 * 60 * 24;
                } else {
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("uuid", UUID.fromString(uuid));
                data.put("expires", exp);
                data.put("reason", reason);
                data.put("name", name);
                data.put("by", by);

                postgres.from("bans").insert(data).thenAccept(result -> {
                    Optional<Player> playerOpt = server.getPlayer(UUID.fromString(uuid));
                    if (playerOpt.isEmpty()) return;
                    Player target = playerOpt.get();

                    target.disconnect(Reason.banReason(result));
                });
            });
        }).start());

        Messenger.listen("unban", message -> postgres.from("bans").eq("uuid", UUID.fromString(message)).delete());
    }

    @Subscribe
    public void beforeJoin(LoginEvent event) {
        postgres.from("bans").eq("uuid", event.getPlayer().getUniqueId()).execute().thenAccept(result -> {
            if (!result.isEmpty()) {
                if (Long.parseLong(result.first().get("expires").toString()) <= System.currentTimeMillis()) {
                    postgres.from("bans").eq("uuid", event.getPlayer().getUniqueId()).delete();
                    return;
                }
                event.getPlayer().disconnect(Reason.banReason(result));
            }
        });
    }
}
