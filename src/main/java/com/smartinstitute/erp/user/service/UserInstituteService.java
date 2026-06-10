package com.smartinstitute.erp.user.service;

import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import com.smartinstitute.erp.user.entity.UserInstitute;
import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.repository.UserInstituteRepository;
import com.smartinstitute.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInstituteService {
    private final UserInstituteRepository userInstituteRepository;
    private final InstituteRepository instituteRepository;
    private final UserRepository userRepository;
    public void assignUserToInstitute(Long userId, Long instituteId, Role role) {

        // ✅ 1. Check institute exists
        if (!instituteRepository.existsById(instituteId)) {
            throw new RuntimeException("Institute not found");
        }

        // ✅ 2. Check user exists
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }

        // ✅ 3. Create mapping
        UserInstitute mapping = new UserInstitute();
        mapping.setUser(userRepository.findById(userId).get());
        mapping.setInstitute(instituteRepository.findById(instituteId).get());
        mapping.setRole(role);

        userInstituteRepository.save(mapping);
    }
}
