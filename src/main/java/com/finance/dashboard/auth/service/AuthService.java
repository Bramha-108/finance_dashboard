package com.finance.dashboard.auth.service;

import com.finance.dashboard.auth.dto.*;
import com.finance.dashboard.auth.model.*;
import com.finance.dashboard.auth.repository.*;
import com.finance.dashboard.shared.exception.ResourceNotFoundException;
import com.finance.dashboard.shared.security.AppUserDetails;
import com.finance.dashboard.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.jwt.expiration-ms}")
    private long accessExpirationMs;

    public TokenResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        Role viewerRole = roleRepo.findByName(RoleName.ROLE_VIEWER)
                .orElseThrow(() -> new IllegalStateException("Default role not found"));

        User user = new User();
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setRoles(Set.of(viewerRole));
        userRepo.save(user);

        AppUserDetails userDetails = new AppUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);
        return TokenResponse.of(accessToken, refreshToken, accessExpirationMs);
    }

    public TokenResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AppUserDetails userDetails = new AppUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);

        refreshTokenRepo.deleteByUser_Id(user.getId());
        String refreshToken = createRefreshToken(user);

        return TokenResponse.of(accessToken, refreshToken, accessExpirationMs);
    }

    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepo.findByToken(rawRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepo.delete(stored);
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = stored.getUser();
        AppUserDetails userDetails = new AppUserDetails(user);
        String newAccess = jwtService.generateToken(userDetails);

        refreshTokenRepo.delete(stored);
        String newRefresh = createRefreshToken(user);

        return TokenResponse.of(newAccess, newRefresh, accessExpirationMs);
    }

    private String createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L));
        return refreshTokenRepo.save(rt).getToken();
    }
}