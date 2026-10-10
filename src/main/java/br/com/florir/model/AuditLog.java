package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "manager_id", nullable = false)
  private Manager manager;

  @Column(name = "action", nullable = false)
  private String action;

  @Column(name = "entity", nullable = false)
  private String entity;

  @Column(name = "entity_id", nullable = false)
  private String entityId;
}
