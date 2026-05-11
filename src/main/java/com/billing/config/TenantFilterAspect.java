package com.billing.config;

import com.billing.model.User;
import com.billing.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public TenantFilterAspect(EntityManager entityManager, UserRepository userRepository) {
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }

    @Before("execution(* com.billing.repository.*.*(..)) && !execution(* com.billing.repository.UserRepository.*(..)) && !execution(* com.billing.repository.BusinessProfileRepository.*(..)) && !execution(* com.billing.repository.OtpTokenRepository.*(..))")
    public void enableTenantFilter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            Optional<User> userOpt = userRepository.findByUsernameAndActiveTrue(username);
            if (userOpt.isPresent() && userOpt.get().getBusinessProfile() != null) {
                Long tenantId = userOpt.get().getBusinessProfile().getId();
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            }
        }
    }
}
