package dev.pgm.community.users;

import dev.pgm.community.Community;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.feature.SessionFeature;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class UserProfileImpl implements UserProfile {

  private UUID playerId;
  private String username;
  private Instant firstLogin;
  private Instant lastLogin;
  private int joinCount;

  public UserProfileImpl(UUID playerId, String username) {
    this(playerId, username, Instant.now(), null, 1);
  }

  public UserProfileImpl(
      UUID playerId,
      String username,
      Instant firstLogin,
      @Nullable Instant lastLogin,
      int joinCount) {
    this.playerId = playerId;
    this.username = username;
    this.firstLogin = firstLogin;
    this.lastLogin = lastLogin;
    this.joinCount = joinCount;
  }

  @Override
  public UUID getId() {
    return playerId;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public Instant getFirstLogin() {
    return firstLogin;
  }

  @Override
  public Instant getLastLogin() {
    return lastLogin;
  }

  @Override
  public CompletableFuture<Session> getLatestSession(boolean ignoreDisguisedSessions) {
    SessionFeature sessions = Community.get().getFeatures().getSessions();
    if (!sessions.isEnabled()) return CompletableFuture.completedFuture(null);

    return sessions.getLatestSession(getId(), ignoreDisguisedSessions);
  }

  @Override
  public int getJoinCount() {
    return joinCount;
  }

  @Override
  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public void setFirstLogin(Instant now) {
    this.firstLogin = now;
  }

  @Override
  public void incJoinCount() {
    this.joinCount++;
  }
}
