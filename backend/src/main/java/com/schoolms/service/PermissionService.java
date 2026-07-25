package com.schoolms.service;

import com.schoolms.repository.PermissionRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public boolean userHasPermission(Long userId, String permissionKey) {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant == null) return false;
        try {
            return permissionRepository.userHasPermission(userId, tenant, permissionKey);
        } catch (Exception e) {
            return false;
        }
    }
}
