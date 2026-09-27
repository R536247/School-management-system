package com.schoolms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attendance")
public class Attendance extends BaseEntity {
    @Column(name = "entity_type", nullable = false)
    private String entityType; // 'student' or 'employee'

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "status", nullable = false)
    private String status; // 'present', 'absent', 'late'

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
}
