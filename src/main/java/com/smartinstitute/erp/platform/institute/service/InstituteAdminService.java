package com.smartinstitute.erp.platform.institute.service;

import com.smartinstitute.erp.platform.institute.dto.InstituteResponseDto;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.entity.UserInstitute;
import com.smartinstitute.erp.user.repository.UserInstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstituteAdminService {

    private final UserInstituteRepository userInstituteRepo;
    private final InstituteRepository instituteRepo;

    // ✅ Get logged-in admin's institute
    public InstituteResponseDto getMyInstitute() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        UserInstitute ui = userInstituteRepo.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        Institute institute = ui.getInstitute();

        return mapToDto(institute);
    }

    // ✅ Update institute
    public InstituteResponseDto updateInstitute(InstituteResponseDto dto) {

        Institute institute = instituteRepo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        institute.setName(dto.getName());
        institute.setAddress(dto.getAddress());
        institute.setPhone(dto.getPhone());
        institute.setEmail(dto.getEmail());
        institute.setLogoUrl(dto.getLogoUrl());

        instituteRepo.save(institute);

        return mapToDto(institute);
    }

    // ✅ Mapper
    private InstituteResponseDto mapToDto(Institute institute) {
        InstituteResponseDto dto = new InstituteResponseDto();
        dto.setId(institute.getId());
        dto.setName(institute.getName());
        dto.setAddress(institute.getAddress());
        dto.setPhone(institute.getPhone());
        dto.setEmail(institute.getEmail());
        dto.setLogoUrl(institute.getLogoUrl());
        dto.setActive(institute.isActive());
        return dto;
    }
}