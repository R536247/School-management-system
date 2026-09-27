package com.schoolms.controller;

import com.schoolms.service.DashboardService;
import com.schoolms.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final PermissionService permissionService;

    public DashboardController(DashboardService dashboardService, PermissionService permissionService) {
        this.dashboardService = dashboardService;
        this.permissionService = permissionService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "dashboard.view")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
