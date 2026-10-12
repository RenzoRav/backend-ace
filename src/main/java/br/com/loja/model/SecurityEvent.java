package br.com.loja.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "security_event")
@Getter
@Setter
@NoArgsConstructor
public class SecurityEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String event;

  @Column(nullable = false, length = 32)
  private String result;

  @Column(name = "actor_reference", nullable = false, length = 64)
  private String actorReference;

  @Column(name = "correlation_id", nullable = false, length = 64)
  private String correlationId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
