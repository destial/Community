package dev.pgm.community.feature;

import co.aikar.commands.BukkitCommandManager;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.assistance.feature.types.SQLAssistanceFeature;
import dev.pgm.community.broadcast.BroadcastFeature;
import dev.pgm.community.chat.management.ChatManagementFeature;
import dev.pgm.community.chat.network.NetworkChatFeature;
import dev.pgm.community.commands.CommunityPluginCommand;
import dev.pgm.community.commands.ContainerCommand;
import dev.pgm.community.commands.FlightCommand;
import dev.pgm.community.commands.GamemodeCommand;
import dev.pgm.community.commands.ServerInfoCommand;
import dev.pgm.community.commands.StaffCommand;
import dev.pgm.community.commands.SudoCommand;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.freeze.FreezeFeature;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.friends.feature.types.SQLFriendshipFeature;
import dev.pgm.community.info.InfoCommandsFeature;
import dev.pgm.community.mobs.MobFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.feature.types.SQLModerationFeature;
import dev.pgm.community.motd.MotdFeature;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.types.RedisNetworkFeature;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.nick.feature.types.SQLNickFeature;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.requests.feature.types.SQLRequestFeature;
import dev.pgm.community.sessions.feature.SessionFeature;
import dev.pgm.community.sessions.feature.types.SQLSessionFeature;
import dev.pgm.community.teleports.TeleportFeature;
import dev.pgm.community.teleports.TeleportFeatureBase;
import dev.pgm.community.translations.TranslationFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.users.feature.types.SQLUsersFeature;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.vanish.VanishFeature;
import fr.minuskube.inv.InventoryManager;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/** Manages all {@link Feature}s of the plugin */
public class FeatureManager {

  private final AssistanceFeature reports;
  private final ModerationFeature moderation;
  private final UsersFeature users;
  private final FriendshipFeature friends;
  private final NetworkFeature network;
  private final NickFeature nick;
  private final RequestFeature requests;
  private final SessionFeature sessions;

  private final TeleportFeature teleports;
  private final InfoCommandsFeature infoCommands;
  private final ChatManagementFeature chatManagement;
  private final NetworkChatFeature chatNetwork;
  private final MotdFeature motd;
  private final FreezeFeature freeze;
  private final MutationFeature mutation;
  private final BroadcastFeature broadcast;
  private final VanishFeature vanish;
  private final TranslationFeature translation;
  private final MobFeature mob;

  public FeatureManager(
      Configuration config,
      Logger logger,
      DatabaseConnection database,
      BukkitCommandManager commands,
      InventoryManager inventory) {
    // Networking
    this.network = new RedisNetworkFeature(config, logger);

    // DB Features
    this.users = new SQLUsersFeature(config, logger, database);
    this.sessions = new SQLSessionFeature(users, logger, database);
    this.reports = new SQLAssistanceFeature(config, logger, database, users, network, inventory);
    this.moderation = new SQLModerationFeature(config, logger, database, users, network);
    this.friends = new SQLFriendshipFeature(config, logger, database, users);
    this.nick = new SQLNickFeature(config, logger, database, users);
    this.requests = new SQLRequestFeature(config, logger, database, users);

    // TODO: 1. Add support for non-persist database (e.g NoDBUsersFeature)
    // TODO: 2. Support non-sql databases?
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
    // Not a priority

    // Non-DB Features
    this.teleports = new TeleportFeatureBase(config, logger);
    this.infoCommands = new InfoCommandsFeature(config, logger);
    this.chatManagement = new ChatManagementFeature(config, logger);
    this.motd = new MotdFeature(config, logger);
    this.freeze = new FreezeFeature(config, logger);
    this.mutation = new MutationFeature(config, logger, inventory);
    this.broadcast = new BroadcastFeature(config, logger);
    this.vanish = new VanishFeature(config, logger, nick);
    this.chatNetwork = new NetworkChatFeature(config, logger, network);
    this.translation = new TranslationFeature(config, logger);
    this.mob = new MobFeature(config, logger);

    this.registerCommands(commands);
  }

  public AssistanceFeature getReports() {
    return reports;
  }

  public ModerationFeature getModeration() {
    return moderation;
  }

  public UsersFeature getUsers() {
    return users;
  }

  public SessionFeature getSessions() {
    return sessions;
  }

  public TeleportFeature getTeleports() {
    return teleports;
  }

  public InfoCommandsFeature getInfoCommands() {
    return infoCommands;
  }

  public ChatManagementFeature getChatManagement() {
    return chatManagement;
  }

  public FriendshipFeature getFriendships() {
    return friends;
  }

  public MotdFeature getMotd() {
    return motd;
  }

  public FreezeFeature getFreeze() {
    return freeze;
  }

  public MutationFeature getMutations() {
    return mutation;
  }

  public NickFeature getNick() {
    return nick;
  }

  public BroadcastFeature getBroadcast() {
    return broadcast;
  }

  public VanishFeature getVanish() {
    return vanish;
  }

  public NetworkChatFeature getNetworkChat() {
    return chatNetwork;
  }

  public RequestFeature getRequests() {
    return requests;
  }

  public TranslationFeature getTranslations() {
    return translation;
  }

  public MobFeature getMobs() {
    return mob;
  }

  // Register Feature commands and any dependency
  private void registerCommands(BukkitCommandManager commands) {
    // Dependency injection for features
    commands.registerDependency(UsersFeature.class, getUsers());
    commands.registerDependency(AssistanceFeature.class, getReports());
    commands.registerDependency(ModerationFeature.class, getModeration());
    commands.registerDependency(SessionFeature.class, getSessions());
    commands.registerDependency(TeleportFeature.class, getTeleports());
    commands.registerDependency(ChatManagementFeature.class, getChatManagement());
    commands.registerDependency(FriendshipFeature.class, getFriendships());
    commands.registerDependency(FreezeFeature.class, getFreeze());
    commands.registerDependency(MutationFeature.class, getMutations());
    commands.registerDependency(BroadcastFeature.class, getBroadcast());
    commands.registerDependency(NickFeature.class, getNick());
    commands.registerDependency(VanishFeature.class, getVanish());
    commands.registerDependency(RequestFeature.class, getRequests());
    commands.registerDependency(TranslationFeature.class, getTranslations());
    commands.registerDependency(MobFeature.class, getMobs());

    // Custom command completions
    commands
        .getCommandCompletions()
        .registerCompletion(
            "mutes",
            x ->
                getModeration().getOnlineMutes().stream()
                    .map(Player::getName)
                    .collect(Collectors.toSet()));

    commands
        .getCommandCompletions()
        .registerCompletion(
            "addMutations",
            x ->
                Stream.of(MutationType.values())
                    .filter(mt -> !getMutations().hasMutation(mt))
                    .map(MutationType::name)
                    .collect(Collectors.toSet()));
    commands
        .getCommandCompletions()
        .registerCompletion(
            "removeMutations",
            x ->
                Stream.of(MutationType.values())
                    .filter(mt -> getMutations().hasMutation(mt))
                    .map(MutationType::name)
                    .collect(Collectors.toSet()));

    commands.getCommandCompletions().registerCompletion("maps", x -> PGMUtils.getMapNames());
    commands
        .getCommandCompletions()
        .registerCompletion("allowedMaps", x -> PGMUtils.getAllowedMapNames());

    commands
        .getCommandCompletions()
        .registerCompletion(
            "mobs",
            c ->
                Arrays.asList(EntityType.values()).stream()
                    .filter(EntityType::isAlive)
                    .filter(EntityType::isSpawnable)
                    .map(EntityType::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList()));

    // Feature commands
    registerFeatureCommands(getUsers(), commands);
    registerFeatureCommands(getReports(), commands);
    registerFeatureCommands(getModeration(), commands);
    registerFeatureCommands(getSessions(), commands);
    registerFeatureCommands(getTeleports(), commands);
    registerFeatureCommands(getChatManagement(), commands);
    registerFeatureCommands(getFriendships(), commands);
    registerFeatureCommands(getFreeze(), commands);
    registerFeatureCommands(getMutations(), commands);
    registerFeatureCommands(getBroadcast(), commands);
    registerFeatureCommands(getNick(), commands);
    registerFeatureCommands(getVanish(), commands);
    registerFeatureCommands(getRequests(), commands);
    registerFeatureCommands(getTranslations(), commands);
    registerFeatureCommands(getMobs(), commands);
    // TODO: Group calls together and perform upon reload
    // will allow commands to be enabled/disabled with features

    // Other commands
    commands.registerCommand(new CommunityPluginCommand());
    commands.registerCommand(new FlightCommand());
    commands.registerCommand(new StaffCommand());
    commands.registerCommand(new GamemodeCommand());
    commands.registerCommand(new ServerInfoCommand());
    commands.registerCommand(new ContainerCommand());
    commands.registerCommand(new SudoCommand());
  }

  private void registerFeatureCommands(Feature feature, BukkitCommandManager commandManager) {
    feature.getCommands().forEach(commandManager::registerCommand);
  }

  public void reloadConfig(Configuration config) {
    // Reload all config values here
    getReports().getConfig().reload(config);
    getModeration().getConfig().reload(config);
    getUsers().getConfig().reload(config);
    getSessions().getConfig().reload(config);
    getTeleports().getConfig().reload(config);
    getInfoCommands().getConfig().reload(config);
    getChatManagement().getConfig().reload(config);
    getMotd().getConfig().reload(config);
    getFreeze().getConfig().reload(config);
    getMutations().getConfig().reload(config);
    getBroadcast().getConfig().reload(config);
    getNick().getConfig().reload(config);
    getVanish().getConfig().reload(config);
    getNetworkChat().getConfig().reload(config);
    getRequests().getConfig().reload(config);
    getTranslations().getConfig().reload(config);
    getMobs().getConfig().reload(config);
    // TODO: Look into maybe unregister commands for features that have been disabled
    // commands#unregisterCommand
    // Will need to check isEnabled
  }

  public void disable() {
    if (getReports().isEnabled()) getReports().disable();
    if (getModeration().isEnabled()) getModeration().disable();
    if (getUsers().isEnabled()) getUsers().disable();
    if (getSessions().isEnabled()) getSessions().disable();
    if (getTeleports().isEnabled()) getTeleports().disable();
    if (getInfoCommands().isEnabled()) getInfoCommands().disable();
    if (getChatManagement().isEnabled()) getChatManagement().disable();
    if (getMotd().isEnabled()) getMotd().disable();
    if (getFreeze().isEnabled()) getFreeze().disable();
    if (getMutations().isEnabled()) getMutations().disable();
    if (getBroadcast().isEnabled()) getBroadcast().disable();
    if (getNick().isEnabled()) getNick().disable();
    if (getVanish().isEnabled()) getVanish().disable();
    if (getNetworkChat().isEnabled()) getNetworkChat().disable();
    if (getRequests().isEnabled()) getRequests().disable();
    if (getTranslations().getConfig().isEnabled()) getTranslations().disable();
    if (getMobs().isEnabled()) getMobs().disable();
  }
}
