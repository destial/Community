package dev.pgm.community.utils;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextTranslations;

public class MessageUtils {

  public static final String TOKEN_SYMBOL = "✪";
  public static final Component DENY = text("\u2715", NamedTextColor.DARK_RED);
  public static final Component ACCEPT = text("\u2714", NamedTextColor.GREEN);
  public static final Component WARNING = text("\u26a0", NamedTextColor.YELLOW);
  public static final Component TOKEN = text(TOKEN_SYMBOL, NamedTextColor.GOLD);

  public static String formatKickScreenMessage(String headerTitle, List<Component> lines) {
    List<Component> message = Lists.newArrayList();

    Component header =
        text(LegacyFormatUtils.horizontalLineHeading(headerTitle, ChatColor.DARK_GRAY));

    Component footer =
        text(
            LegacyFormatUtils.horizontalLine(
                ChatColor.DARK_GRAY, LegacyFormatUtils.MAX_CHAT_WIDTH));

    message.add(header); // Header Line - FIRST
    lines.forEach(message::add); // Add messages
    message.add(footer); // Footer Line - LAST

    return TextTranslations.translateLegacy(
        Component.join(text("\n" + ChatColor.RESET), message), null);
  }

  public static Component formatUnseen(String target) {
    return text()
        .append(text(target, NamedTextColor.DARK_AQUA))
        .append(text(" has never joined the server", NamedTextColor.RED))
        .build();
    // TODO: translate
  }

  public static Component formatLastSeen(
      UUID playerId,
      String username,
      boolean online,
      boolean visible,
      Instant lastSeen,
      boolean sameServer,
      String server) {
    return text()
        .append(PlayerComponent.player(playerId, username, NameStyle.FANCY))
        .append(text(visible ? " has been online for " : " was last seen ")) // TODO: translate
        .append(
            (visible
                    ? TemporalComponent.duration(Duration.between(lastSeen, Instant.now())).build()
                    : TemporalComponent.relativePastApproximate(lastSeen))
                .color(online ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN))
        .append(text(sameServer ? "" : " on "))
        .append(
            text(sameServer ? "" : server)
                .color(online ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN))
        .color(NamedTextColor.GRAY)
        .build();
  }

  public static Component formatTokenTransaction(int amount, Component message) {
    boolean add = amount > 0;
    return text()
        .append(
            text(
                add ? "+" : "-",
                add ? NamedTextColor.GREEN : NamedTextColor.RED,
                TextDecoration.BOLD))
        .append(text(Math.abs(amount) + " ", NamedTextColor.YELLOW, TextDecoration.BOLD))
        .append(TOKEN)
        .append(space())
        .append(message)
        .build();
  }

  public static List<String> colorizeList(List<String> list) {
    return list.stream().map(BukkitUtils::colorize).collect(Collectors.toList());
  }

  public static String format(String format, Object... args) {
    return String.format(
        ChatColor.translateAlternateColorCodes('&', format != null ? format : ""), args);
  }
}
