package com.schoolms.controller;

import com.schoolms.entity.ClassRoom;
import com.schoolms.service.ClassService;
import com.schoolms.service.PermissionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {
    private final ClassService classService;
    private final PermissionService permissionService;

    public ClassController(ClassService classService, PermissionService permissionService) {
        this.classService = classService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<ClassRoom> list() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "classes.view")) {
            throw new org.springframework.security.access.AccessDeniedException("Forbidden");
        }
        return classService.list();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ClassRoom c) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "classes.create")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        ClassRoom created = classService.create(c);
        return ResponseEntity.created(URI.create("/api/v1/classes/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "classes.view")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Optional<ClassRoom> c = classService.get(id);
        return c.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ClassRoom c) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "classes.update")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Optional<ClassRoom> o = classService.update(id, c);
        return o.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "classes.delete")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        boolean ok = classService.delete(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
