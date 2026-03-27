package hu.jgj52.huTiersBans.Utils;

import hu.jgj52.databaseVelocity.PostgreSQL;
import net.kyori.adventure.text.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class Reason {
    public static Component banReason(PostgreSQL.QueryResult result) {
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

        return Component.text(
                "§cKi vagy tiltva a szerverről:\n\n" +
                        "§c████████" + "\n" +
                        "§c███§f██§c███\n" +
                        "§c███§f██§c███\n" +
                        "§c███§f██§c███\n" +
                        "§c███§f██§c███\n" +
                        "§c███§f██§c███\n" +
                        "§c███§f██§c███\n" +
                        "§c████████\n" +
                        "§c███§f██§c███\n" +
                        "§c████████\n\n" +
                        "§7Oka: §f" + r.replaceAll("&", "§") + "\n" +
                        "§7Adta: §6" + ban.get("by").toString() + "\n" +
                        "§7Lejár: §6" + expires + "\n" +
                        "§7(§6" + in + "§7)"
        );
    }
}
