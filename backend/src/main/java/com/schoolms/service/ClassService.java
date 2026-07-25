package com.schoolms.service;

import com.schoolms.entity.ClassRoom;
import com.schoolms.repository.ClassRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClassService {
    private final ClassRepository classRepository;

    public ClassService(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    public List<ClassRoom> list() {
        Long tenant = TenantContext.getCurrentTenant();
        return classRepository.findAllBySchoolId(tenant);
    }

    public Optional<ClassRoom> get(Long id) {
        Optional<ClassRoom> c = classRepository.findById(id);
        if (c.isEmpty() || !TenantContext.getCurrentTenant().equals(c.get().getSchoolId())) return Optional.empty();
        return c;
    }

    public ClassRoom create(ClassRoom c) {
        c.setSchoolId(TenantContext.getCurrentTenant());
        return classRepository.save(c);
    }

    public Optional<ClassRoom> update(Long id, ClassRoom updated) {
        Optional<ClassRoom> ex = get(id);
        if (ex.isEmpty()) return Optional.empty();
        ClassRoom e = ex.get();
        e.setName(updated.getName());
        e.setCode(updated.getCode());
        e.setDescription(updated.getDescription());
        return Optional.of(classRepository.save(e));
    }

    public boolean delete(Long id) {
        Optional<ClassRoom> ex = get(id);
        if (ex.isEmpty()) return false;
        classRepository.deleteById(id);
        return true;
    }
}
