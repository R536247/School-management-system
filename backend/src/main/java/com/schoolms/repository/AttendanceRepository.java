package com.schoolms.repository;

import com.schoolms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySchoolIdAndDate(Long schoolId, LocalDate date);
    List<Attendance> findBySchoolIdAndDateBetween(Long schoolId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findBySchoolIdAndEntityTypeAndEntityId(Long schoolId, String entityType, Long entityId);
    Optional<Attendance> findFirstBySchoolIdAndEntityTypeAndEntityIdAndDate(
            Long schoolId, String entityType, Long entityId, LocalDate date);
}
