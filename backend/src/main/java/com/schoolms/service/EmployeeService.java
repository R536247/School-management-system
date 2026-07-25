package com.schoolms.service;

import com.schoolms.entity.Employee;
import com.schoolms.repository.EmployeeRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Page<Employee> list(Pageable pageable) {
        Long tenant = TenantContext.getCurrentTenant();
        return employeeRepository.findAllBySchoolId(tenant, pageable);
    }

    public Optional<Employee> get(Long id) {
        Long tenant = TenantContext.getCurrentTenant();
        Optional<Employee> e = employeeRepository.findById(id);
        if (e.isEmpty() || !tenant.equals(e.get().getSchoolId())) return Optional.empty();
        return e;
    }

    public Employee create(Employee e) {
        Long tenant = TenantContext.getCurrentTenant();
        e.setSchoolId(tenant);
        return employeeRepository.save(e);
    }

    public Optional<Employee> update(Long id, Employee updated) {
        Optional<Employee> existing = get(id);
        if (existing.isEmpty()) return Optional.empty();
        Employee ex = existing.get();
        ex.setFirstName(updated.getFirstName());
        ex.setLastName(updated.getLastName());
        ex.setDepartment(updated.getDepartment());
        ex.setRoleTitle(updated.getRoleTitle());
        ex.setSalary(updated.getSalary());
        ex.setHireDate(updated.getHireDate());
        ex.setMeta(updated.getMeta());
        ex.setStatus(updated.getStatus());
        return Optional.of(employeeRepository.save(ex));
    }

    public boolean delete(Long id) {
        Optional<Employee> existing = get(id);
        if (existing.isEmpty()) return false;
        employeeRepository.deleteById(id);
        return true;
    }
}
