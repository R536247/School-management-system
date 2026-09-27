package com.schoolms.controller;

import com.schoolms.entity.Attendance;
import com.schoolms.service.AttendanceService;
import com.schoolms.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final PermissionService permissionService;

    public AttendanceController(AttendanceService attendanceService, PermissionService permissionService) {
        this.attendanceService = attendanceService;
        this.permissionService = permissionService;
    }

    @PostMapping("/mark")
    public ResponseEntity<?> mark(@RequestBody Attendance a) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "attendance.mark")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Attendance marked = attendanceService.mark(a);
        return ResponseEntity.ok(marked);
    }

    @GetMapping
    public ResponseEntity<?> getForDate(@RequestParam LocalDate date,
                                        @RequestParam String entityType) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "attendance.view")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.ok(attendanceService.getForDate(date, entityType));
    }
}
