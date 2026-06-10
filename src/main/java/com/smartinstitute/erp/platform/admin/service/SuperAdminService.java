package com.smartinstitute.erp.platform.admin.service;

import com.smartinstitute.erp.platform.admin.dto.InstituteUserDto;
import com.smartinstitute.erp.platform.admin.dto.InstituteUsersResponseDto;
import com.smartinstitute.erp.platform.institute.RequestStatus;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.entity.InstituteRequest;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import com.smartinstitute.erp.platform.institute.repository.InstituteRequestRepository;
import com.smartinstitute.erp.platform.institute.service.InstituteOnboardingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    @PersistenceContext
    private EntityManager entityManager;

    private final InstituteRepository instituteRepo;
    private final InstituteRequestRepository requestRepo;
    private final InstituteOnboardingService onboardingService;

    //  1. Get all institutes
    public List<Institute> getAllInstitutes() {
        return instituteRepo.findAll();
    }

    //  2. Toggle Active / Inactive
    public void toggleInstitute(Long id) {

        Institute institute = instituteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        institute.setActive(!institute.isActive()); // toggle true/false
        instituteRepo.save(institute);
    }

    //  3. Get pending requests
    public List<InstituteRequest> getPendingRequests() {
        return requestRepo.findByStatus(RequestStatus.PENDING);
    }

    //  4. Approve request
    public void approveRequest(Long requestId) {
        onboardingService.approve(requestId);
    }

    //  5. Reject request (optional but recommended)
    public void rejectRequest(Long requestId) {

        InstituteRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus(RequestStatus.REJECTED);
        requestRepo.save(request);
    }

    /** Get students, teachers, principals for an institute (Super Admin view). Uses native SQL to avoid entity filters. */
    @Transactional(readOnly = true)
    public InstituteUsersResponseDto getInstituteUsers(Long instituteId) {
        Institute institute = instituteRepo.findById(instituteId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        List<InstituteUserDto> students = fetchUsersByNativeQuery(
                "SELECT s.id, u.name, u.email FROM students s INNER JOIN users u ON s.user_id = u.id WHERE s.institute_id = :instituteId",
                instituteId,
                "STUDENT"
        );
        List<InstituteUserDto> teachers = fetchUsersByNativeQuery(
                "SELECT t.id, u.name, u.email FROM teachers t INNER JOIN users u ON t.user_id = u.id WHERE t.institute_id = :instituteId",
                instituteId,
                "TEACHER"
        );
        List<InstituteUserDto> principals = fetchUsersByNativeQuery(
                "SELECT p.id, u.name, u.email FROM principals p INNER JOIN users u ON p.user_id = u.id WHERE p.institute_id = :instituteId",
                instituteId,
                "PRINCIPAL"
        );

        return new InstituteUsersResponseDto(
                institute.getId(),
                institute.getName(),
                students,
                teachers,
                principals
        );
    }

    @SuppressWarnings("unchecked")
    private List<InstituteUserDto> fetchUsersByNativeQuery(String sql, Long instituteId, String role) {
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("instituteId", instituteId);
        List<Object[]> rows = q.getResultList();
        List<InstituteUserDto> list = new ArrayList<>();
        for (Object[] row : rows) {
            Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
            String name = row[1] != null ? row[1].toString() : "-";
            String email = row[2] != null ? row[2].toString() : "-";
            list.add(new InstituteUserDto(id, name, email, role));
        }
        return list;
    }
}