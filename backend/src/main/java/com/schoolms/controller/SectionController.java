package com.schoolms.controller;

import com.schoolms.entity.Section;
import com.schoolms.service.SectionService;
import com.schoolms.service.PermissionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sections")
public class SectionController {
    private final SectionService sectionService;
    private final PermissionService permissionService;

    public SectionController(SectionService sectionService, PermissionService permissionService) {
        this.sectionService = sectionService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<Section> list() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "sections.view")) {
            throw new org.springframework.security.access.AccessDeniedException("Forbidden");
        }
        return sectionService.list();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Section s) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "sections.create")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Section created = sectionService.create(s);
        return ResponseEntity.created(URI.create("/api/v1/sections/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "sections.view")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Optional<Section> s = sectionService.get(id);
        return s.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Section s) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "sections.update")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        Optional<Section> o = sectionService.update(id, s);
        return o.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        Long userId = principal instanceof Long ? (Long) principal : null;
        if (userId == null || !permissionService.userHasPermission(userId, "sections.delete")) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        boolean ok = sectionService.delete(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
