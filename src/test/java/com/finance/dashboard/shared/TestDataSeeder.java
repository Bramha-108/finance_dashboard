package com.finance.dashboard.shared;

import com.finance.dashboard.auth.model.Role;
import com.finance.dashboard.auth.model.RoleName;
import com.finance.dashboard.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestDataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (roleRepository.count() == 0) {
            for (RoleName name : RoleName.values()) {
                Role role = new Role();
                role.setName(name);
                roleRepository.save(role);
            }
        }
    }
}