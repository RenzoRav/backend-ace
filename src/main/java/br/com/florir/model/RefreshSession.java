package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "refresh_session")
public class RefreshSession extends BaseEntity {
  @Column(name = "role", nullable = false)
  private String role;

  public String getRole() {
    return role;
  }

  public void setRole(String value) {
    this.role = value;
  }

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long value) {
    this.accountId = value;
  }

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String value) {
    this.tokenHash = value;
  }

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant value) {
    this.expiresAt = value;
  }

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  public boolean getRevoked() {
    return revoked;
  }

  public void setRevoked(boolean value) {
    this.revoked = value;
  }

  @Column(nullable = false)
  private long credentialVersion;

  public long getCredentialVersion() {
    return credentialVersion;
  }

  public void setCredentialVersion(long v) {
    credentialVersion = v;
  }
}
