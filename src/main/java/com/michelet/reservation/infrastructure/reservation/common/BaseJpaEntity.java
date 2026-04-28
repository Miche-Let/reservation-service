package com.michelet.reservation.infrastructure.reservation.common;

import com.michelet.common.entity.BaseEntity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@MappedSuperclass
public abstract class BaseJpaEntity extends BaseEntity   // common 라이브러리 BaseEntity
    implements Persistable<UUID> {

  @Transient
  private boolean isNew = false;

  @Override
  public boolean isNew() { return isNew; }

  protected void markNew() { this.isNew = true; }

  @PostPersist
  @PostLoad
  protected void markNotNew() {
    this.isNew = false;
  }
}
