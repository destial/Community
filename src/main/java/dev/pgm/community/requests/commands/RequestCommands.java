package dev.pgm.community.requests.commands;

import static dev.pgm.community.utils.PGMUtils.parseMapText;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

public class RequestCommands extends CommunityCommand {

  @Dependency private RequestFeature requests;
  @Dependency private UsersFeature users;

  @CommandAlias("request|req")
  @Description("Request a map")
  @Syntax("[map] - Name of map to request")
  @CommandCompletion("@maps")
  @CommandPermission(CommunityPermissions.REQUEST)
  public void request(CommandAudience audience, Player player, String mapName) {
    requests.request(player, parseMapText(mapName));
  }

  @CommandAlias("sponsor")
  @Description("View the sponsor request menu")
  public class SponsorCommands extends CommunityCommand {

    @Subcommand("cancel")
    public void cancel(CommandAudience audience, Player player) {
      if (requests.cancelSponsorRequest(player.getUniqueId())) {
        audience.sendMessage(text("Removed sponsor request!", NamedTextColor.GREEN));
      } else {
        audience.sendWarning(text("You don't have any pending sponsor requests to cancel"));
      }
    }

    @Default
    @Subcommand("info")
    public void info(CommandAudience audience, Player player) {
      Component header =
          TextFormatter.horizontalLineHeading(
              audience.getSender(),
              text("Sponsor", NamedTextColor.YELLOW, TextDecoration.BOLD),
              NamedTextColor.GOLD,
              TextFormatter.MAX_CHAT_WIDTH);

      Component footer =
          TextFormatter.horizontalLine(NamedTextColor.GOLD, TextFormatter.MAX_CHAT_WIDTH);

      requests
          .getRequestProfile(player.getUniqueId())
          .thenAcceptAsync(
              profile -> {
                Component tokenBalance =
                    text()
                        .append(RequestFeature.TOKEN)
                        .append(text("Token balance: "))
                        .append(text(profile.getSponsorTokens(), NamedTextColor.YELLOW))
                        .append(text(" / "))
                        .append(
                            text(
                                ((RequestConfig) requests.getConfig()).getMaxTokens(),
                                NamedTextColor.GOLD))
                        .append(renderRefreshTime(player, profile))
                        .color(NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.runCommand("/tokens"))
                        .hoverEvent(
                            HoverEvent.showText(text("Click to view tokens", NamedTextColor.GRAY)))
                        .build();

                Component buttons =
                    text()
                        .append(text("              "))
                        .append(
                            button(
                                "Maps",
                                NamedTextColor.DARK_AQUA,
                                "/sponsor maps 1",
                                "Click to view available maps"))
                        .append(text("                  "))
                        .append(
                            button(
                                "Queue",
                                NamedTextColor.DARK_GREEN,
                                "/queue",
                                "View a list of waiting sponsor requests ("
                                    + ChatColor.YELLOW
                                    + requests.getSponsorQueue().size()
                                    + ChatColor.GRAY
                                    + ")"))
                        .build();

                audience.sendMessage(header);

                // TOKEN BALANCE
                audience.sendMessage(tokenBalance);

                // Existing request status
                requests
                    .getPendingSponsor(player.getUniqueId())
                    .ifPresent(
                        sponsor -> {
                          int queueIndex = requests.queueIndex(sponsor);
                          boolean next = queueIndex == 0;

                          Component current =
                              text("Current Request: ", NamedTextColor.GRAY, TextDecoration.BOLD)
                                  .append(
                                      button(
                                          "Cancel",
                                          NamedTextColor.RED,
                                          "/sponsor cancel",
                                          "Click to cancel this request"));
                          Component queue =
                              next
                                  ? text(
                                      "(Will be added to the next vote)",
                                      NamedTextColor.GRAY,
                                      TextDecoration.ITALIC)
                                  : text()
                                      .append(text("(Queue location: "))
                                      .append(text("#" + queueIndex, NamedTextColor.YELLOW))
                                      .append(text(")"))
                                      .color(NamedTextColor.GRAY)
                                      .hoverEvent(
                                          HoverEvent.showText(
                                              text(
                                                  "Your request's location in the queue",
                                                  NamedTextColor.GRAY)))
                                      .build();

                          audience.sendMessage(empty());
                          audience.sendMessage(current);
                          audience.sendMessage(
                              text()
                                  .append(text(" - ", NamedTextColor.YELLOW))
                                  .append(
                                      sponsor
                                          .getMap()
                                          .getStyledName(MapNameStyle.COLOR_WITH_AUTHORS))
                                  .build());
                          audience.sendMessage(queue);
                        });

                audience.sendMessage(empty());

                // Cooldown message
                if (!requests.canSponsor(player.getUniqueId())) {
                  audience.sendMessage(
                      text()
                          .append(text("Cooldown", NamedTextColor.GOLD, TextDecoration.BOLD))
                          .append(text(": ", NamedTextColor.GRAY))
                          .append(
                              MessageUtils.formatTimeLeft(
                                  ((RequestConfig) requests.getConfig()).getSponsorCooldown(),
                                  profile.getLastSponsorTime(),
                                  NamedTextColor.RED))
                          .color(NamedTextColor.GRAY)
                          .hoverEvent(
                              HoverEvent.showText(
                                  text(
                                      "Time until you can sponsor another map",
                                      NamedTextColor.GRAY)))
                          .build());
                  audience.sendMessage(empty());
                }

                // [Maps] [Queue]
                audience.sendMessage(buttons);

                audience.sendMessage(footer);
              });
    }

    @Subcommand("request|submit|add")
    @Description("Sponsor a map request")
    @Syntax("[map] - Name of map to sponsor")
    @CommandCompletion("@allowedMaps")
    public void sponsor(CommandAudience audience, Player player, @Optional String mapName) {
      if (mapName != null) {
        requests.sponsor(player, parseMapText(mapName));
      } else {
        audience.sendWarning(text("Please provide a map to sponsor."));
      }
    }

    @Subcommand("maps")
    @Description("View a list of maps which can be sponsored")
    public void viewMapList(CommandAudience audience, @Default("1") int page) {
      Set<MapInfo> maps =
          Sets.newHashSet(PGM.get().getMapLibrary().getMaps()).stream()
              .filter(PGMUtils::isMapSizeAllowed)
              .filter(m -> m.getPhase() != Phase.DEVELOPMENT)
              .collect(Collectors.toSet());

      int resultsPerPage = 8;
      int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

      Component paginated =
          TextFormatter.paginate(
              text("Available Maps"),
              page,
              pages,
              NamedTextColor.DARK_AQUA,
              NamedTextColor.AQUA,
              true);

      Component formattedTitle =
          TextFormatter.horizontalLineHeading(
              audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

      new PaginatedComponentResults<MapInfo>(formattedTitle, resultsPerPage) {
        @Override
        public Component format(MapInfo map, int index) {
          Component mapName =
              map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)
                  .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
                  .hoverEvent(
                      HoverEvent.showText(
                          translatable(
                              "command.maps.hover",
                              NamedTextColor.GRAY,
                              map.getStyledName(MapNameStyle.COLOR))));

          return text()
              .append(text((index + 1) + ". "))
              .append(mapName)
              .append(renderSponsorButton(audience.getSender(), map))
              .color(NamedTextColor.GRAY)
              .build();
        }

        @Override
        public Component formatEmpty() {
          return text("There are no available maps to sponsor!", NamedTextColor.RED);
        }
      }.display(audience.getAudience(), maps, page);

      // Add page button when more than 1 page
      if (pages > 1) {
        TextComponent.Builder buttons = text();

        if (page > 1) {
          buttons.append(
              text()
                  .append(BroadcastUtils.LEFT_DIV.color(NamedTextColor.GOLD))
                  .append(text(" Previous Page", NamedTextColor.BLUE))
                  .hoverEvent(
                      HoverEvent.showText(text("Click to view previous page", NamedTextColor.GRAY)))
                  .clickEvent(ClickEvent.runCommand("/sponsor maps " + (page - 1))));
        }

        if (page > 1 && page < pages) {
          buttons.append(text(" | ", NamedTextColor.DARK_GRAY));
        }

        if (page < pages) {
          buttons.append(
              text()
                  .append(text("Next Page ", NamedTextColor.BLUE))
                  .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
                  .hoverEvent(
                      HoverEvent.showText(text("Click to view next page", NamedTextColor.GRAY)))
                  .clickEvent(ClickEvent.runCommand("/sponsor maps " + (page + 1))));
        }
        audience.sendMessage(
            TextFormatter.horizontalLineHeading(
                audience.getSender(), buttons.build(), NamedTextColor.DARK_PURPLE, 250));
      }
    }

    @CommandAlias("queue|sponsorqueue|sq")
    @Subcommand("queue")
    @Description("View the sponsored maps queue")
    public void viewQueue(CommandAudience audience, @Default("1") int page) {
      Queue<SponsorRequest> queue = requests.getSponsorQueue();

      int resultsPerPage = ((RequestConfig) requests.getConfig()).getMaxQueue();
      int pages = (queue.size() + resultsPerPage - 1) / resultsPerPage;

      Component paginated =
          TextFormatter.paginate(
              text("Sponsor Queue"),
              page,
              pages,
              NamedTextColor.DARK_AQUA,
              NamedTextColor.AQUA,
              true);

      Component formattedTitle =
          TextFormatter.horizontalLineHeading(
              audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

      new PaginatedComponentResults<SponsorRequest>(formattedTitle, resultsPerPage) {
        @Override
        public Component format(SponsorRequest sponsor, int index) {
          MapInfo map = sponsor.getMap();
          Component mapName =
              map.getStyledName(MapNameStyle.COLOR)
                  .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
                  .hoverEvent(
                      HoverEvent.showText(
                          translatable(
                              "command.maps.hover",
                              NamedTextColor.GRAY,
                              map.getStyledName(MapNameStyle.COLOR))));

          Component playerName =
              VisibilityUtils.isDisguised(sponsor.getPlayerId())
                  ? empty()
                  : text()
                      .append(BroadcastUtils.BROADCAST_DIV)
                      .append(
                          player(
                              sponsor.getPlayerId(),
                              users.getUsername(sponsor.getPlayerId()),
                              NameStyle.FANCY))
                      .build();

          return text()
              .append(text((index + 1) + ". "))
              .append(mapName)
              .append(playerName)
              .color(NamedTextColor.GRAY)
              .build();
        }

        @Override
        public Component formatEmpty() {
          return text("There are no maps in the sponsor queue!", NamedTextColor.RED);
        }
      }.display(audience.getAudience(), queue, page);
    }

    private Component renderRefreshTime(Player player, RequestProfile profile) {
      RequestConfig config = (RequestConfig) requests.getConfig();
      TokenRefreshAmount info = getTimeLeft(player, profile.getLastTokenRefreshTime());

      if (info.getDuration() != null) {
        boolean canClaim = profile.getSponsorTokens() < config.getMaxTokens();

        if (canClaim) {
          return text()
              .append(text(" | "))
              .append(text("Next token ("))
              .append(text("+" + info.getAmount(), NamedTextColor.GREEN, TextDecoration.BOLD))
              .append(text("): "))
              .append(
                  info.getDuration().isNegative()
                      ? text("Now! Please rejoin")
                      : duration(info.getDuration(), NamedTextColor.YELLOW))
              .build();
        } else {
          return text()
              .append(text(" | No tokens to claim"))
              .hoverEvent(
                  HoverEvent.showText(
                      text(
                          "You have the max amount of sponsor tokens! To claim more you need to spend some first.",
                          NamedTextColor.RED)))
              .build();
        }
      }

      return empty();
    }
  }

  @CommandAlias("requests|reqs")
  @Description("View and manage map requests")
  @CommandPermission(CommunityPermissions.STAFF)
  public class StaffRequestCommands extends CommunityCommand {

    @Default
    public void listRequests(CommandAudience audience, @Default("1") int page) {
      Map<MapInfo, Integer> requestCounts = requests.getRequests();

      int resultsPerPage = 8;
      int pages = (requestCounts.size() + resultsPerPage - 1) / resultsPerPage;

      Component paginated =
          TextFormatter.paginate(
              text("Map Requests"),
              page,
              pages,
              NamedTextColor.DARK_AQUA,
              NamedTextColor.AQUA,
              true);

      Component formattedTitle =
          TextFormatter.horizontalLineHeading(
              audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

      new PaginatedComponentResults<MapInfo>(formattedTitle, resultsPerPage) {
        @Override
        public Component format(MapInfo map, int index) {
          Component mapName =
              map.getStyledName(MapNameStyle.COLOR)
                  .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
                  .hoverEvent(
                      HoverEvent.showText(
                          translatable(
                              "command.maps.hover",
                              NamedTextColor.GRAY,
                              map.getStyledName(MapNameStyle.COLOR))));

          int requestCount = requestCounts.get(map);
          Component count = text(requestCount, NamedTextColor.DARK_AQUA, TextDecoration.BOLD);

          Component setButton =
              getRequestsButton(
                  "Set", "Click to setnext", NamedTextColor.GREEN, "/sn " + map.getName());
          Component voteButton =
              getRequestsButton(
                  "Vote",
                  "Click to add to the next vote",
                  NamedTextColor.LIGHT_PURPLE,
                  "/vote add " + map.getName());
          Component removeButton =
              getRequestsButton(
                  "\u2715",
                  "Click to remove all requests",
                  NamedTextColor.RED,
                  "/reqs clear " + map.getName());

          return text()
              .append(text((index + 1) + ". "))
              .append(mapName)
              .append(BroadcastUtils.BROADCAST_DIV)
              .append(count)
              .append(text(" request" + (requestCount != 1 ? "s " : " ")))
              .append(setButton)
              .append(space())
              .append(voteButton)
              .append(space())
              .append(removeButton)
              .color(NamedTextColor.GRAY)
              .build();
        }

        @Override
        public Component formatEmpty() {
          return text("No map requests have been made!", NamedTextColor.RED);
        }
      }.display(audience.getAudience(), requestCounts.keySet(), page);
    }

    @Subcommand("clear")
    @Syntax("[map name] - Empty to clear all")
    @Description("Clear map requests")
    public void clearRequests(CommandAudience audience, @Optional String mapName) {
      if (mapName != null) {
        MapInfo map = parseMapText(mapName);
        int removalCount = requests.clearRequests(map);
        Component removed =
            text()
                .append(text(" ("))
                .append(text(removalCount, NamedTextColor.RED))
                .append(text(")"))
                .color(NamedTextColor.GRAY)
                .build();
        BroadcastUtils.sendAdminChatMessage(
            text()
                .append(audience.getStyledName())
                .append(text(" has cleared all requests for "))
                .append(map.getStyledName(MapNameStyle.COLOR))
                .append(removed)
                .color(NamedTextColor.GRAY)
                .build());
      } else {
        requests.clearAllRequests();
        BroadcastUtils.sendAdminChatMessage(
            text()
                .append(audience.getStyledName())
                .append(text(" has cleared all map requests", NamedTextColor.GRAY))
                .build());
      }
    }
  }

  private Component getRequestsButton(
      String text, String hover, NamedTextColor color, String command) {
    return text()
        .append(text("["))
        .append(text(text, color))
        .append(text("]"))
        .hoverEvent(HoverEvent.showText(text(hover, color)))
        .clickEvent(ClickEvent.runCommand(command))
        .build();
  }

  private Component renderSponsorButton(CommandSender sender, MapInfo map) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      if (requests.getCached(player.getUniqueId()) != null) {
        RequestProfile profile = requests.getCached(player.getUniqueId());
        if (profile.getSponsorTokens() > 0) {
          return text()
              .append(BroadcastUtils.BROADCAST_DIV)
              .append(text("["))
              .append(RequestFeature.SPONSOR)
              .append(text("]"))
              .hoverEvent(
                  HoverEvent.showText(
                      text("Click to sponsor ", NamedTextColor.GRAY)
                          .append(map.getStyledName(MapNameStyle.COLOR))))
              .clickEvent(ClickEvent.runCommand("/sponsor add " + map.getName()))
              .color(NamedTextColor.GRAY)
              .build();
        }
      }
    }
    return empty();
  }

  @CommandAlias("tokens|sponsortokens|token")
  @Description("View how many sponsor tokens you have")
  public class TokenCommands extends CommunityCommand {

    @Subcommand("give|award")
    @Syntax("[player] [token amount] - Amount of tokens to award")
    @Description("Give the targeted player sponsor tokens")
    @CommandCompletion("@players")
    @CommandPermission(CommunityPermissions.ADMIN)
    public void give(CommandAudience audience, String target, int amount) {
      getTarget(target, users)
          .thenAcceptAsync(
              targetId -> {
                if (targetId.isPresent()) {
                  RequestProfile profile = requests.getRequestProfile(targetId.get()).join();
                  if (profile != null) {
                    int total = profile.award(amount);
                    requests.update(profile);
                    audience.sendMessage(
                        text()
                            .append(MessageUtils.TOKEN)
                            .append(space())
                            .append(
                                users.renderUsername(profile.getPlayerId(), NameStyle.FANCY).join())
                            .append(text(" now has "))
                            .append(text(total, NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(text(" sponsor token" + (total != 1 ? "s" : "")))
                            .color(NamedTextColor.GRAY)
                            .build());
                    return;
                  }
                }
                audience.sendWarning(formatNotFoundComponent(target));
              });
    }

    @Default
    @Subcommand("balance")
    @Description("Check your token balance")
    public void tokens(CommandAudience audience, Player player) {
      requests
          .getRequestProfile(player.getUniqueId())
          .thenAcceptAsync(
              profile -> {
                int tokens = profile.getSponsorTokens();
                audience.sendMessage(
                    text()
                        .append(MessageUtils.TOKEN)
                        .append(text(" You have "))
                        .append(text(tokens, NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .append(text(" sponsor token" + (tokens != 1 ? "s" : "") + "."))
                        .color(NamedTextColor.GRAY)
                        .build());
                sendRefreshDuration(audience.getAudience(), player, profile);
              });
    }
  }

  private class TokenRefreshAmount {
    private Duration timeLeft;
    private int amount;

    public TokenRefreshAmount(Duration timeLeft, int amount) {
      this.timeLeft = timeLeft;
      this.amount = amount;
    }

    public Duration getDuration() {
      return timeLeft;
    }

    public int getAmount() {
      return amount;
    }
  }

  private TokenRefreshAmount getTimeLeft(Player player, Instant lastRefresh) {
    RequestConfig config = (RequestConfig) requests.getConfig();
    int amount = 0;
    Duration timeLeft = null;
    if (player.hasPermission(CommunityPermissions.TOKEN_DAILY)) {
      amount = config.getDailyTokenAmount();
      timeLeft = Duration.ofDays(1).minus(Duration.between(lastRefresh, Instant.now()));
    } else if (player.hasPermission(CommunityPermissions.TOKEN_WEEKLY)) {
      amount = config.getWeeklyTokenAmount();
      timeLeft = Duration.ofDays(7).minus(Duration.between(lastRefresh, Instant.now()));
    }
    return new TokenRefreshAmount(timeLeft, amount);
  }

  private void sendRefreshDuration(Audience viewer, Player player, RequestProfile profile) {
    if (!player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)) return;

    if (profile.getLastTokenRefreshTime() == null) {
      viewer.sendWarning(
          text("Please rejoin the server to claim more tokens!", NamedTextColor.GOLD));
      return;
    }

    TokenRefreshAmount info = getTimeLeft(player, profile.getLastTokenRefreshTime());
    if (info.getDuration() != null) {
      if (profile.getSponsorTokens() >= ((RequestConfig) requests.getConfig()).getMaxTokens()) {
        viewer.sendMessage(text("Spend some tokens in order to claim more", NamedTextColor.GRAY));
        return;
      }

      if (info.getDuration().isNegative()) {
        viewer.sendMessage(
            text("Token refresh ready! Please rejoin the server to claim", NamedTextColor.GRAY));
        return;
      }

      viewer.sendMessage(
          text()
              .append(text("-  ", NamedTextColor.YELLOW))
              .append(text("Next token ("))
              .append(text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
              .append(text(info.getAmount(), NamedTextColor.GREEN, TextDecoration.BOLD))
              .append(text(") in "))
              .append(duration(info.getDuration(), NamedTextColor.YELLOW))
              .color(NamedTextColor.GRAY)
              .build());
    }
  }
}
