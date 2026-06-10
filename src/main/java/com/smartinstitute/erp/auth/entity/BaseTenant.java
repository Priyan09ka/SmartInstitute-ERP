package com.smartinstitute.erp.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "instituteId", type = Long.class))
@Filter(
        name = "tenantFilter",
        condition = "institute_id = :instituteId")
@Getter
@Setter
public abstract class BaseTenant {

    @Column(name = "institute_id", nullable = false)
    private Long instituteId;
}