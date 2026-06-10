package com.smartinstitute.erp.academic.course.service;

import com.smartinstitute.erp.academic.course.dto.CourseDto;
import com.smartinstitute.erp.academic.course.entity.Course;
import com.smartinstitute.erp.academic.course.repository.CourseRepository;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final InstituteRepository instituteRepository;

    public CourseDto create(CourseDto dto) {
        Long tenantId = TenantContext.getTenantId();

        if (courseRepository.existsByNameAndInstitute_Id(dto.getName(), tenantId)) {
            throw new RuntimeException("Course already exists");
        }

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));


        Course course = new Course();
        course.setName(dto.getName());
        course.setCode(dto.getCode());
        course.setDescription(dto.getDescription());
        course.setInstitute(institute);

        courseRepository.save(course);

        return toDto(course);
    }
    public List<CourseDto> list() {

        Long tenantId = TenantContext.getTenantId();

        return courseRepository.findByInstitute_Id(tenantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public CourseDto get(Long id) {

        Long tenantId = TenantContext.getTenantId();

        Course course = courseRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        return toDto(course);
    }


    public CourseDto update(Long id, CourseDto dto) {

        Long tenantId = TenantContext.getTenantId();

        Course course = courseRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        course.setName(dto.getName());
        course.setCode(dto.getCode());
        course.setDescription(dto.getDescription());

        courseRepository.save(course);

        return toDto(course);
    }


    public void delete(Long id) {

        Long tenantId = TenantContext.getTenantId();

        Course course = courseRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        courseRepository.delete(course);
    }

    private CourseDto toDto(Course course) {

        CourseDto dto = new CourseDto();
        dto.setId(course.getId());
        dto.setName(course.getName());
        dto.setCode(course.getCode());
        dto.setDescription(course.getDescription());

        return dto;
    }
}