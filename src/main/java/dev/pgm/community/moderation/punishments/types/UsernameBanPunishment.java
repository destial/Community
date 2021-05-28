package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class UsernameBanPunishment extends Punishment {

  public UsernameBanPunishment(
      UUID punishmentId,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      String service) {
    super(
        PunishmentType.NAME_BAN,
        punishmentId,
        targetId,
        issuerId,
        reason,
        null,
        timeIssued,
        active,
        lastUpdated,
        lastUpdatedBy,
        service);
  }

  @Override
  public boolean punish(boolean silent) {
    return kick(silent);
  }
}
