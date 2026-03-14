package hu.jgj52.huTiersBans;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
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
            player.sendMessage(Component.text("§cNincs " + targetName + " nevű játékos"));
            return;
        }

        Player target = targetOpt.get();

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        long exp = System.currentTimeMillis();

        if (args[1].endsWith("s")) {
            exp += Long.parseLong(args[1].replaceAll("s", "")) * 1000;
        } else if (args[1].endsWith("m")) {
            exp += Long.parseLong(args[1].replaceAll("m", "")) * 1000 * 60;
        } else if (args[1].endsWith("h")) {
            exp += Long.parseLong(args[1].replaceAll("h", "")) * 1000 * 60 * 60;
        } else if (args[1].endsWith("d")) {
            exp += Long.parseLong(args[1].replaceAll("d", "")) * 1000 * 60 * 60 * 24;
        } else {
            player.sendMessage(Component.text("§cOda kell írnod a szám mögé azt, hogy miben adod meg. §fs§c, §fm§c, §fh§c vagy §fd§c"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("uuid", target.getUniqueId());
        data.put("expires", exp);
        data.put("reason", reason);
        data.put("name", target.getUsername());
        data.put("by", player.getUsername());

        postgres.from("bans").insert(data).thenAccept(result -> {
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
            }

            String expires = df.format(new Date(Long.parseLong(ban.get("expires").toString())));
            String in = sb.toString().trim();

            String r = ban.get("reason").toString();

            target.disconnect(Component.text(
                    "§cKi vagy tiltva a szerverről:\n" +
                            "§7Oka: §f" + r.replaceAll("&", "§") + "\n" +
                            "§7Lejár: §6" + expires + "\n" +
                            "§7azaz §6" + in + " múlva"
            ));
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