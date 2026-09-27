package com.schoolms.repository;

import com.schoolms.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findAllBySchoolId(Long schoolId, Pageable pageable);
    boolean existsByIdAndSchoolId(Long id, Long schoolId);
}
