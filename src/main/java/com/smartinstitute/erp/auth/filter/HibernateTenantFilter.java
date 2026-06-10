package com.smartinstitute.erp.auth.filter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class HibernateTenantFilter {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableFilter(Long instituteId) {
        if (instituteId == null) return;

        Session session = entityManager.unwrap(Session.class);

        session.enableFilter("tenantFilter")
                .setParameter("instituteId", instituteId);
    }

    public void disable() {
        try {
            entityManager.unwrap(Session.class)
                    .disableFilter("tenantFilter");
        } catch (Exception ignored) {
        }
    }
}