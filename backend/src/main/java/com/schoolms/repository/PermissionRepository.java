package com.schoolms.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionRepository {

    @PersistenceContext
    private EntityManager em;

    public boolean userHasPermission(Long userId, Long schoolId, String permissionKey) {
        String sql = "SELECT EXISTS (\n" +
                "  SELECT 1 FROM role_permissions rp\n" +
                "  JOIN permissions p ON p.id = rp.permission_id\n" +
                "  JOIN user_roles ur ON ur.role_id = rp.role_id\n" +
                "  WHERE ur.user_id = :userId AND p.key = :permissionKey AND ur.school_id = :schoolId\n" +
                ")";

        Object result = em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("schoolId", schoolId)
                .setParameter("permissionKey", permissionKey)
                .getSingleResult();

        if (result instanceof Boolean) return (Boolean) result;
        if (result instanceof Number) return ((Number) result).intValue() != 0;
        return Boolean.parseBoolean(String.valueOf(result));
    }
}
