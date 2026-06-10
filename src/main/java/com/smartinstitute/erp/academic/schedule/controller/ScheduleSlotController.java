package com.smartinstitute.erp.academic.schedule.controller;

import com.smartinstitute.erp.academic.schedule.dto.ScheduleSlotRequestDto;
import com.smartinstitute.erp.academic.schedule.dto.ScheduleSlotResponseDto;
import com.smartinstitute.erp.academic.schedule.service.ScheduleSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
public class ScheduleSlotController {

    private final ScheduleSlotService service;

    @PostMapping
    public ScheduleSlotResponseDto create(@RequestBody ScheduleSlotRequestDto dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<ScheduleSlotResponseDto> list() {
        return service.list();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
