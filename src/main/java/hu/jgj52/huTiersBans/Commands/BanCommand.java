package hu.jgj52.huTiersBans.Commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import hu.jgj52.huTiersBans.HuTiersBans;
import hu.jgj52.huTiersBans.Utils.Reason;
import net.kyori.adventure.text.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static hu.jgj52.databaseVelocity.DatabaseVelocity.postgres;

public class BanCommand implements SimpleCommand {
    private final HuTiersBans plugin;

    public BanCommand(HuTiersBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hutiers.bans.ban");
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) return;

        String[] args = invocation.arguments();
        if (args.length < 3) {
            player.sendMessage(Component.text("/ban <játékos> <idő> <ok>"));
            return;
        }

        String targetName = args[0];

        Optional<Player> targetOpt = plugin.server.getPlayer(targetName);

        if (targetOpt.isEmpty()) {
            String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

            long now = System.currentTimeMillis();

            long exp = now;

            if (args[1].endsWith("s")) {
                exp += Long.parseLong(args[1].replaceAll("s", "")) * 1000;
            } else if (args[1].endsWith("m")) {
                exp += Long.parseLong(args[1].replaceAll("m", "")) * 1000 * 60;
            } else if (args[1].endsWith("h")) {
                exp += Long.parseLong(args[1].replaceAll("h", "")) * 1000 * 60 * 60;
            } else if (args[1].endsWith("d")) {
                exp += Long.parseLong(args[1].replaceAll("d", "")) * 1000 * 60 * 60 * 24;
            } else {
                player.sendMessage(Component.text("§cOda kell írnod a szám mögé azt, hogy miben adod meg. §fs§c (másodperc), §fm§c (perc), §fh§c (óra) vagy §fd§c (nap)"));
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("expires", exp);
            data.put("reason", reason);
            data.put("name", args[0]);
            data.put("by", player.getUsername());
            data.put("got", now);

            postgres.from("bans").insert(data).thenAccept(result -> player.sendMessage(Component.text("§a" + args[0] + " sikeresen ki lett tiltva.")));
            return;
        }

        Player target = targetOpt.get();

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        long now = System.currentTimeMillis();

        long exp = now;

        if (args[1].endsWith("s")) {
            exp += Long.parseLong(args[1].replaceAll("s", "")) * 1000;
        } else if (args[1].endsWith("m")) {
            exp += Long.parseLong(args[1].replaceAll("m", "")) * 1000 * 60;
        } else if (args[1].endsWith("h")) {
            exp += Long.parseLong(args[1].replaceAll("h", "")) * 1000 * 60 * 60;
        } else if (args[1].endsWith("d")) {
            exp += Long.parseLong(args[1].replaceAll("d", "")) * 1000 * 60 * 60 * 24;
        } else {
            player.sendMessage(Component.text("§cOda kell írnod a szám mögé azt, hogy miben adod meg. §fs§c (másodperc), §fm§c (perc), §fh§c (óra) vagy §fd§c (nap)"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("uuid", target.getUniqueId());
        data.put("expires", exp);
        data.put("reason", reason);
        data.put("name", target.getUsername());
        data.put("by", player.getUsername());
        data.put("got", now);

        postgres.from("bans").insert(data).thenAccept(result -> {
            target.disconnect(Reason.banReason(result));
            player.sendMessage(Component.text("§a" + target.getUsername() + " sikeresen ki lett tiltva."));
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0 || args.length == 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();

            return plugin.server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}