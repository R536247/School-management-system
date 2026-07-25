package com.schoolms.tenant;

import com.schoolms.entity.BaseEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;

public class TenantEntityListener {

    @PrePersist
    public void prePersist(BaseEntity entity) {
        if (entity.getSchoolId() == null) {
            Long tenant = TenantContext.getCurrentTenant();
            entity.setSchoolId(tenant);
        }
        entity.setUpdatedAt(OffsetDateTime.now());
    }

    @PreUpdate
    public void preUpdate(BaseEntity entity) {
        entity.setUpdatedAt(OffsetDateTime.now());
    }
}
