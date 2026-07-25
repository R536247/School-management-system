package com.schoolms.service;

import com.schoolms.entity.Attendance;
import com.schoolms.repository.AttendanceRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public Attendance mark(Attendance a) {
        Long tenant = TenantContext.getCurrentTenant();
        a.setSchoolId(tenant);
        return attendanceRepository.save(a);
    }

    public List<Attendance> getForDate(LocalDate date) {
        Long tenant = TenantContext.getCurrentTenant();
        return attendanceRepository.findBySchoolIdAndDate(tenant, date);
    }

    public List<Attendance> getForEntity(String entityType, Long entityId) {
        Long tenant = TenantContext.getCurrentTenant();
        return attendanceRepository.findBySchoolIdAndEntityTypeAndEntityId(tenant, entityType, entityId);
    }
}
