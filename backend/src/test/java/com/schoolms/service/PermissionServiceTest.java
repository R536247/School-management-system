package com.schoolms.service;

import com.schoolms.repository.PermissionRepository;
import com.schoolms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionServiceTest {

    @Mock
    PermissionRepository permissionRepository;

    private PermissionService permissionService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        permissionService = new PermissionService(permissionRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        TenantContext.clear();
        mocks.close();
    }

    @Test
    void userHasPermission_returnsTrue() {
        TenantContext.setCurrentTenant(1L);
        when(permissionRepository.userHasPermission(10L, 1L, "students.view")).thenReturn(true);
        assertTrue(permissionService.userHasPermission(10L, "students.view"));
        verify(permissionRepository, times(1)).userHasPermission(10L, 1L, "students.view");
    }

    @Test
    void userHasPermission_noTenant_returnsFalse() {
        TenantContext.clear();
        assertFalse(permissionService.userHasPermission(10L, "students.view"));
        verifyNoInteractions(permissionRepository);
    }
}
