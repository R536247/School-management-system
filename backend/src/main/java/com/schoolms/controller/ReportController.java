package com.schoolms.controller;

import com.schoolms.entity.Attendance;
import com.schoolms.entity.Student;
import com.schoolms.repository.AttendanceRepository;
import com.schoolms.repository.StudentRepository;
import com.schoolms.service.PermissionService;
import com.schoolms.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final PermissionService permissionService;

    public ReportController(AttendanceRepository attendanceRepository,
                            StudentRepository studentRepository,
                            PermissionService permissionService) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<?> getReport(@RequestParam String type,
                                       @RequestParam LocalDate startDate,
                                       @RequestParam LocalDate endDate) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "reports.view")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body("End date must not be before start date");
        }

        Long schoolId = TenantContext.getCurrentTenant();
        if ("attendance".equals(type)) {
            List<Attendance> records = attendanceRepository
                    .findBySchoolIdAndDateBetween(schoolId, startDate, endDate);
            return ResponseEntity.ok(records);
        }
        if ("students".equals(type)) {
            List<Student> students = studentRepository
                    .findAllBySchoolId(schoolId, org.springframework.data.domain.Pageable.unpaged())
                    .getContent();
            return ResponseEntity.ok(students);
        }
        return ResponseEntity.badRequest().body("This report type is not supported by the available data");
    }
}