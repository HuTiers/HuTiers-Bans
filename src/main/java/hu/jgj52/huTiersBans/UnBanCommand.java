package hu.jgj52.huTiersBans;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import hu.jgj52.databaseVelocity.PostgreSQL;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static hu.jgj52.databaseVelocity.DatabaseVelocity.postgres;

public class UnBanCommand implements SimpleCommand {
    private final HuTiersBans plugin;

    public UnBanCommand(HuTiersBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hutiers.bans.unban");
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) return;

        String[] args = invocation.arguments();
        if (args.length == 0) {
            player.sendMessage(Component.text("/unban <játékos>"));
            return;
        }

        String targetName = args[0];

        plugin.server.getScheduler().buildTask(plugin, () -> {
            try {
                PostgreSQL.QueryResult result = postgres.from("bans")
                        .eq("name", targetName)
                        .execute()
                        .get();

                if (result.isEmpty()) {
                    player.sendMessage(Component.text(targetName + " nincs kitiltva."));
                    return;
                }

                postgres.from("bans").eq("name", targetName).delete();

                player.sendMessage(Component.text("§a" + targetName + " kitiltása sikeresen fel lett oldva."));

            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage(Component.text("Nem sikerült unbanolni " + targetName + "-t"));
            }
        }).schedule();
    }


    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0 || args.length == 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();

            try {
                PostgreSQL.QueryResult result = postgres
                        .from("bans")
                        .execute()
                        .get();

                if (result.isEmpty()) return Collections.emptyList();

                return result.data.stream()
                        .map(row -> row.get("name"))
                        .filter(obj -> obj instanceof String)
                        .map(obj -> ((String) obj))
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());

            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

}
