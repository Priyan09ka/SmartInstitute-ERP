package com.smartinstitute.erp.platform.institute.controller;

import com.smartinstitute.erp.platform.institute.RequestStatus;
import com.smartinstitute.erp.platform.institute.dto.InstituteRequestDto;
import com.smartinstitute.erp.platform.institute.entity.InstituteRequest;
import com.smartinstitute.erp.platform.institute.repository.InstituteRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class InstituteRequestPublicController {

    private final InstituteRequestRepository requestRepo;

    @PostMapping("/institute-request")
    public InstituteRequest create(@RequestBody InstituteRequestDto dto) {

        String pwd = dto.getPassword() != null ? dto.getPassword().trim() : "";
        String confirm = dto.getConfirmPassword() != null ? dto.getConfirmPassword().trim() : "";
        if (!pwd.isEmpty() || !confirm.isEmpty()) {
            if (pwd.length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters");
            }
            if (!pwd.equals(confirm)) {
                throw new RuntimeException("Password and confirm password do not match");
            }
        }

        InstituteRequest req = new InstituteRequest();
        req.setInstituteName(dto.getInstituteName());
        req.setAdminName(dto.getAdminName());
        req.setAdminEmail(dto.getAdminEmail());
        req.setPassword(pwd.isEmpty() ? null : pwd);
        req.setAddress(dto.getAddress());
        req.setPhone(dto.getPhone());
        req.setLogoUrl(dto.getLogoUrl());
        req.setStatus(RequestStatus.PENDING);

        return requestRepo.save(req);
    }
    @GetMapping("/institute-requests")
    public List<InstituteRequest> getAllRequests() {
        return requestRepo.findAll();
    }
}