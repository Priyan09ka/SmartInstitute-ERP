package com.smartinstitute.erp.platform.institute.service;

import com.smartinstitute.erp.platform.institute.RequestStatus;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.entity.InstituteRequest;
import com.smartinstitute.erp.platform.institute.record.InstituteApprovedEvent;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import com.smartinstitute.erp.platform.institute.repository.InstituteRequestRepository;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.entity.UserInstitute;
import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.enums.Status;
import com.smartinstitute.erp.user.repository.UserInstituteRepository;
import com.smartinstitute.erp.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstituteOnboardingService {

    private final InstituteRequestRepository requestRepo;
    private final InstituteRepository instituteRepo;
    private final UserRepository userRepo;
    private final UserInstituteRepository uiRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void approve(Long requestId) {

        InstituteRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (req.getStatus() != RequestStatus.PENDING)
            throw new RuntimeException("Already processed");



        // 1. Create Institute
        Institute institute = new Institute();
        if (instituteRepo.existsByAddress(req.getAddress())) {
            throw new RuntimeException("Institute with this address already exists");
        }

        institute.setName(req.getInstituteName());
        institute.setLogoUrl(req.getLogoUrl());
        institute.setAddress(req.getAddress());
        instituteRepo.save(institute);

        // 2. Create or Load User (use password from request if provided, else generate)
        String rawPassword = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : generateDummyPassword();
        User user = userRepo.findByEmail(req.getAdminEmail())
                .orElseGet(() -> {
                    User u = new User();
                    u.setName(req.getAdminName() != null && !req.getAdminName().isBlank()
                            ? req.getAdminName() : req.getAdminEmail());
                    u.setEmail(req.getAdminEmail());
                    u.setPassword(passwordEncoder.encode(rawPassword));
                    u.setStatus(Status.ACTIVE);
                    u.setRole(Role.INSTITUTE_ADMIN);
                    u.setFirstLogin(true);
                    return userRepo.save(u);
                });

        // 3. Map user → institute
        UserInstitute ui = new UserInstitute();
        ui.setUser(user);
        ui.setInstitute(institute);
        ui.setRole(Role.INSTITUTE_ADMIN);
        uiRepo.save(ui);

        // 4. Mark request approved
        req.setStatus(RequestStatus.APPROVED);
        requestRepo.save(req);

        // 5. Fire async mail event
        eventPublisher.publishEvent(
                new InstituteApprovedEvent(
                        user.getEmail(),
                        user.getName(),
                        institute.getName()
                )
        );
    }

    private String generateDummyPassword() {
        return UUID.randomUUID().toString().substring(0, 10);
    }
}