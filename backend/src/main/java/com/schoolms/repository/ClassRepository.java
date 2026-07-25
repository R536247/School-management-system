package com.schoolms.repository;

import com.schoolms.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<ClassRoom, Long> {
    List<ClassRoom> findAllBySchoolId(Long schoolId);
}
