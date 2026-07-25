package com.schoolms.service;

import com.schoolms.entity.Student;
import com.schoolms.repository.StudentRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Page<Student> list(Pageable pageable) {
        Long tenant = TenantContext.getCurrentTenant();
        return studentRepository.findAllBySchoolId(tenant, pageable);
    }

    public Optional<Student> get(Long id) {
        Long tenant = TenantContext.getCurrentTenant();
        Optional<Student> s = studentRepository.findById(id);
        if (s.isEmpty() || !tenant.equals(s.get().getSchoolId())) return Optional.empty();
        return s;
    }

    public Student create(Student s) {
        Long tenant = TenantContext.getCurrentTenant();
        s.setSchoolId(tenant);
        return studentRepository.save(s);
    }

    public Optional<Student> update(Long id, Student updated) {
        Optional<Student> existing = get(id);
        if (existing.isEmpty()) return Optional.empty();
        Student e = existing.get();
        e.setFirstName(updated.getFirstName());
        e.setLastName(updated.getLastName());
        e.setDob(updated.getDob());
        e.setGender(updated.getGender());
        e.setAdmissionNo(updated.getAdmissionNo());
        e.setClassId(updated.getClassId());
        e.setSectionId(updated.getSectionId());
        e.setParentInfo(updated.getParentInfo());
        e.setCustomFields(updated.getCustomFields());
        e.setPhotoPath(updated.getPhotoPath());
        e.setStatus(updated.getStatus());
        return Optional.of(studentRepository.save(e));
    }

    public boolean delete(Long id) {
        Optional<Student> existing = get(id);
        if (existing.isEmpty()) return false;
        studentRepository.deleteById(id);
        return true;
    }
}
