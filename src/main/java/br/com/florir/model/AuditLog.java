package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="audit_log")
public class AuditLog extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="manager_id",nullable=false)
 private Manager manager;
 public Manager getManager(){return manager;}
 public void setManager(Manager value){this.manager=value;}
 @Column(name="action",nullable=false)
 private String action;
 public String getAction(){return action;}
 public void setAction(String value){this.action=value;}
 @Column(name="entity",nullable=false)
 private String entity;
 public String getEntity(){return entity;}
 public void setEntity(String value){this.entity=value;}
 @Column(name="entity_id",nullable=false)
 private String entityId;
 public String getEntityId(){return entityId;}
 public void setEntityId(String value){this.entityId=value;}
}
