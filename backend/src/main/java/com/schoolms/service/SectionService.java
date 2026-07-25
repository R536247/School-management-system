package com.schoolms.service;

import com.schoolms.entity.Section;
import com.schoolms.repository.SectionRepository;
import com.schoolms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {
    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public List<Section> list() {
        Long tenant = TenantContext.getCurrentTenant();
        return sectionRepository.findAllBySchoolId(tenant);
    }

    public Optional<Section> get(Long id) {
        Optional<Section> s = sectionRepository.findById(id);
        if (s.isEmpty() || !TenantContext.getCurrentTenant().equals(s.get().getSchoolId())) return Optional.empty();
        return s;
    }

    public Section create(Section s) {
        s.setSchoolId(TenantContext.getCurrentTenant());
        return sectionRepository.save(s);
    }

    public Optional<Section> update(Long id, Section updated) {
        Optional<Section> existing = get(id);
        if (existing.isEmpty()) return Optional.empty();
        Section e = existing.get();
        e.setName(updated.getName());
        e.setTeacherId(updated.getTeacherId());
        e.setClassId(updated.getClassId());
        return Optional.of(sectionRepository.save(e));
    }

    public boolean delete(Long id) {
        Optional<Section> existing = get(id);
        if (existing.isEmpty()) return false;
        sectionRepository.deleteById(id);
        return true;
    }
}
