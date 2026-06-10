package com.smartinstitute.erp.academic.principal.service;
import com.smartinstitute.erp.academic.principal.dto.PrincipalRequestDto;
import com.smartinstitute.erp.academic.principal.dto.PrincipalResponseDto;
import com.smartinstitute.erp.academic.principal.repository.PrincipalRepository;
import com.smartinstitute.erp.academic.principal.entity.Principal;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.entity.UserInstitute;
import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.enums.Status;
import com.smartinstitute.erp.user.repository.UserInstituteRepository;
import com.smartinstitute.erp.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class PrincipalService {

    private final PrincipalRepository principalRepository;
    private final UserInstituteRepository userInstituteRepository;
    private final UserRepository userRepository;
    private final InstituteRepository instituteRepository;
    private final PasswordEncoder passwordEncoder;

    public PrincipalResponseDto create(PrincipalRequestDto dto) {

        String pwd = dto.getPassword() != null ? dto.getPassword().trim() : "";
        String confirm = dto.getConfirmPassword() != null ? dto.getConfirmPassword().trim() : "";
        if (pwd.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        if (!pwd.equals(confirm)) {
            throw new RuntimeException("Password and confirm password do not match");
        }

        Long tenantId = TenantContext.getTenantId();

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRole(Role.PRINCIPAL);
        user.setStatus(Status.ACTIVE);

        userRepository.save(user);

        Principal principal = new Principal();
        principal.setUser(user);
        principal.setInstitute(institute);

        principalRepository.save(principal);
        UserInstitute ui=new UserInstitute();
        ui.setUser(user);
        ui.setInstitute(institute);
        ui.setRole(Role.PRINCIPAL);
        userInstituteRepository.save(ui);

        return toDto(principal);
    }

    public List<PrincipalResponseDto> list() {

        Long tenantId = TenantContext.getTenantId();

        return principalRepository.findByInstitute_Id(tenantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public PrincipalResponseDto get(Long id) {

        Long tenantId = TenantContext.getTenantId();

        Principal principal = principalRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Principal not found"));

        return toDto(principal);
    }

    private PrincipalResponseDto toDto(Principal p) {

        PrincipalResponseDto dto = new PrincipalResponseDto();

        dto.setId(p.getId());
        dto.setUserId(p.getUser().getId());
        dto.setName(p.getUser().getName());
        dto.setEmail(p.getUser().getEmail());
        dto.setInstituteName(p.getInstitute().getName());

        return dto;
    }
    @Transactional
    public void deactivatePrincipal(Long id) {

        Long tenantId = TenantContext.getTenantId();

        Principal principal = principalRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Principal not found"));

        User user = principal.getUser();
        user.setStatus(Status.INACTIVE);

        userRepository.save(user);
    }
}