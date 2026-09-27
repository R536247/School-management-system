package com.schoolms.service;

import com.schoolms.repository.StudentRepository;
import com.schoolms.repository.EmployeeRepository;
import com.schoolms.repository.AttendanceRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardService(StudentRepository studentRepository, EmployeeRepository employeeRepository,
                            AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public Map<String, Object> getSummary() {
        Long tenant = TenantContext.getCurrentTenant();
        Map<String, Object> summary = new HashMap<>();
        
        long studentCount = studentRepository.findAllBySchoolId(tenant, Pageable.unpaged()).getTotalElements();
        long employeeCount = employeeRepository.findAllBySchoolId(tenant, Pageable.unpaged()).getTotalElements();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<com.schoolms.entity.Attendance> weekAttendance = attendanceRepository
            .findBySchoolIdAndDateBetween(tenant, weekStart, today);
        long presentToday = weekAttendance.stream()
            .filter(a -> today.equals(a.getDate()) && "present".equals(a.getStatus()))
            .count();
        long markedThisWeek = weekAttendance.size();
        long presentThisWeek = weekAttendance.stream()
            .filter(a -> "present".equals(a.getStatus()))
            .count();
        Map<String, Long> presentByDay = new HashMap<>();
        Map<String, Long> absentByDay = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            LocalDate date = weekStart.with(day);
            if (date.isAfter(today)) continue;
            presentByDay.put(day.name(), weekAttendance.stream()
                .filter(a -> date.equals(a.getDate()) && "present".equals(a.getStatus())).count());
            absentByDay.put(day.name(), weekAttendance.stream()
                .filter(a -> date.equals(a.getDate()) && "absent".equals(a.getStatus())).count());
        }
        
        summary.put("totalStudents", studentCount);
        summary.put("totalEmployees", employeeCount);
        summary.put("presentToday", presentToday);
        summary.put("attendancePercentage", markedThisWeek == 0 ? 0 : Math.round((presentThisWeek * 100.0) / markedThisWeek));
        summary.put("presentByDay", presentByDay);
        summary.put("absentByDay", absentByDay);
        summary.put("timestamp", System.currentTimeMillis());
        
        return summary;
    }
}
