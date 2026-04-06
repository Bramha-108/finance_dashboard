package com.finance.dashboard.shared.util;

import com.finance.dashboard.shared.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {}

    public static boolean hasRole(Authentication auth, String roleName) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleName::equals);
    }

    public static UUID getCurrentUserId(Authentication auth) {
        return ((AppUserDetails) auth.getPrincipal()).getId();
    }
}