package com.schoolms.service;

import com.schoolms.entity.Attendance;
import com.schoolms.repository.AttendanceRepository;
import com.schoolms.repository.EmployeeRepository;
import com.schoolms.repository.StudentRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             StudentRepository studentRepository,
                             EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
    }

    public Attendance mark(Attendance a) {
        Long tenant = TenantContext.getCurrentTenant();
        if (!"student".equals(a.getEntityType()) && !"employee".equals(a.getEntityType())) {
            throw new IllegalArgumentException("Unsupported attendance entity type");
        }
        boolean entityExists = "student".equals(a.getEntityType())
            ? studentRepository.existsByIdAndSchoolId(a.getEntityId(), tenant)
            : employeeRepository.existsByIdAndSchoolId(a.getEntityId(), tenant);
        if (!entityExists) {
            throw new IllegalArgumentException("Attendance entity does not belong to the current school");
        }
        a.setSchoolId(tenant);
        Attendance existing = attendanceRepository
            .findFirstBySchoolIdAndEntityTypeAndEntityIdAndDate(
                tenant, a.getEntityType(), a.getEntityId(), a.getDate())
            .orElse(a);
        existing.setSchoolId(tenant);
        existing.setEntityType(a.getEntityType());
        existing.setEntityId(a.getEntityId());
        existing.setDate(a.getDate());
        existing.setStatus(a.getStatus());
        existing.setMeta(a.getMeta());
        return attendanceRepository.save(existing);
    }

    public List<Attendance> getForDate(LocalDate date) {
        Long tenant = TenantContext.getCurrentTenant();
        return attendanceRepository.findBySchoolIdAndDate(tenant, date);
    }

    public List<Attendance> getForDate(LocalDate date, String entityType) {
        Long tenant = TenantContext.getCurrentTenant();
        if (!"student".equals(entityType) && !"employee".equals(entityType)) {
            throw new IllegalArgumentException("Unsupported attendance entity type");
        }
        return attendanceRepository.findBySchoolIdAndDate(tenant, date).stream()
                .filter(record -> entityType.equals(record.getEntityType()))
                .toList();
    }

    public List<Attendance> getForEntity(String entityType, Long entityId) {
        Long tenant = TenantContext.getCurrentTenant();
        return attendanceRepository.findBySchoolIdAndEntityTypeAndEntityId(tenant, entityType, entityId);
    }
}
