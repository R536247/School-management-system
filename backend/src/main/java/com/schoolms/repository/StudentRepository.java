package com.schoolms.repository;

import com.schoolms.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Page<Student> findAllBySchoolId(Long schoolId, Pageable pageable);
    boolean existsByIdAndSchoolId(Long id, Long schoolId);
}
