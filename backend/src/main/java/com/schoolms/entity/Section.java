package com.schoolms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sections")
public class Section extends BaseEntity {
    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "teacher_id")
    private Long teacherId;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
}
