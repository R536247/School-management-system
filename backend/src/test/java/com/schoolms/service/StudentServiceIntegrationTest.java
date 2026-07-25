package com.schoolms.service;

import com.schoolms.entity.Student;
import com.schoolms.repository.StudentRepository;
import com.schoolms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StudentServiceIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        studentRepository.deleteAll();
    }

    @Test
    void create_setsSchoolId() {
        Student s = new Student();
        s.setFirstName("John");
        s.setLastName("Doe");
        Student created = studentService.create(s);
        assertNotNull(created.getId());
        assertEquals(1L, created.getSchoolId());
    }

    @Test
    void list_filtersBySchoolId() {
        Student s1 = new Student();
        s1.setFirstName("Alice");
        s1.setLastName("Smith");
        studentService.create(s1);

        Page<Student> result = studentService.list(PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void get_forbidsOtherSchoolData() {
        Student s = new Student();
        s.setFirstName("Bob");
        s.setLastName("Jones");
        Student created = studentService.create(s);

        TenantContext.setCurrentTenant(2L); // Different school
        Optional<Student> result = studentService.get(created.getId());
        assertTrue(result.isEmpty());
    }
}
