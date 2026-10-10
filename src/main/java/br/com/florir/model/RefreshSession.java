package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_session")
@Getter
@Setter
@NoArgsConstructor
public class RefreshSession extends BaseEntity {
  @Column(name = "role", nullable = false)
  private String role;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  @Column(nullable = false)
  private long credentialVersion;
}
