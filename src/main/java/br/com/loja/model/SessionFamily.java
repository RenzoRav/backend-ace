package br.com.loja.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_family")
@Getter
@Setter
@NoArgsConstructor
public class SessionFamily {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 16)
  private String role;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "absolute_expires_at", nullable = false)
  private Instant absoluteExpiresAt;

  @Column(nullable = false)
  private boolean revoked;
}
