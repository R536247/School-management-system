package com.schoolms.controller;

import com.schoolms.entity.Employee;
import com.schoolms.service.EmployeeService;
import com.schoolms.service.PermissionService;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final PermissionService permissionService;

    public EmployeeController(EmployeeService employeeService, PermissionService permissionService) {
        this.employeeService = employeeService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Page<Employee> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "employees.view")) {
            throw new org.springframework.security.access.AccessDeniedException("Forbidden");
        }
        return employeeService.list(PageRequest.of(page, size));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Employee e) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "employees.create")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Employee created = employeeService.create(e);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "employees.view")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Optional<Employee> e = employeeService.get(id);
        return e.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Employee e) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "employees.update")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Optional<Employee> o = employeeService.update(id, e);
        return o.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "employees.delete")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        boolean ok = employeeService.delete(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
