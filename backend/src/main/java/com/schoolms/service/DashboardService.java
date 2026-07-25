package com.schoolms.service;

import com.schoolms.repository.StudentRepository;
import com.schoolms.repository.EmployeeRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;

    public DashboardService(StudentRepository studentRepository, EmployeeRepository employeeRepository) {
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
    }

    public Map<String, Object> getSummary() {
        Long tenant = TenantContext.getCurrentTenant();
        Map<String, Object> summary = new HashMap<>();
        
        long studentCount = studentRepository.findAllBySchoolId(tenant, Pageable.unpaged()).getTotalElements();
        long employeeCount = employeeRepository.findAllBySchoolId(tenant, Pageable.unpaged()).getTotalElements();
        
        summary.put("totalStudents", studentCount);
        summary.put("totalEmployees", employeeCount);
        summary.put("timestamp", System.currentTimeMillis());
        
        return summary;
    }
}
