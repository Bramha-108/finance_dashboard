package com.finance.dashboard.auth.repository;

import com.finance.dashboard.auth.model.Role;
import com.finance.dashboard.auth.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}