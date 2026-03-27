package hu.jgj52.huTiersBans.Commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import hu.jgj52.huTiersBans.HuTiersBans;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class KickCommand implements SimpleCommand {
    private final HuTiersBans plugin;

    public KickCommand(HuTiersBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hutiers.bans.kick");
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) return;

        String[] args = invocation.arguments();
        if (args.length == 0) {
            player.sendMessage(Component.text("/kick <játékos>"));
            return;
        }

        String targetName = args[0];

        Optional<Player> targetOpt = plugin.server.getPlayer(targetName);

        if (targetOpt.isEmpty()) {
            player.sendMessage(Component.text("§cNincs " + targetName + " nevű játékos"));
            return;
        }

        Player target = targetOpt.get();

        String reason = "§c" + player.getUsername() + " kirúgott";
        if (args.length > 1) {
            reason = reason + ":\n§f" + String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        reason = reason.replaceAll("&", "§");

        target.disconnect(Component.text(reason));
        player.sendMessage(Component.text("§aKirúgtad " + target.getUsername() + " játékost."));
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

