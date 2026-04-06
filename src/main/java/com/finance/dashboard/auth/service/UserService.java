package com.finance.dashboard.auth.service;

import com.finance.dashboard.auth.dto.UserResponse;
import com.finance.dashboard.auth.model.*;
import com.finance.dashboard.auth.repository.RoleRepository;
import com.finance.dashboard.auth.repository.UserRepository;
import com.finance.dashboard.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepo.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse setStatus(UUID id, UserStatus status) {
        User user = getUser(id);
        user.setStatus(status);
        return toResponse(userRepo.save(user));
    }

    public UserResponse assignRole(UUID id, RoleName roleName) {
        User user = getUser(id);
        Role role = roleRepo.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.getRoles().add(role);
        return toResponse(userRepo.save(user));
    }

    public UserResponse revokeRole(UUID id, RoleName roleName) {
        User user = getUser(id);
        user.getRoles().removeIf(r -> r.getName() == roleName);
        return toResponse(userRepo.save(user));
    }

    private User getUser(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getFullName(), u.getStatus(),
                u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()),
                u.getCreatedAt()
        );
    }
}