package com.finance.dashboard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.auth.model.Role;
import com.finance.dashboard.auth.model.RoleName;
import com.finance.dashboard.auth.model.User;
import com.finance.dashboard.auth.model.UserStatus;
import com.finance.dashboard.auth.repository.RoleRepository;
import com.finance.dashboard.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.HashSet;
import java.util.Set;

@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    protected User createUser(String email, String password, RoleName... roleNames) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName("Test User");
        user.setStatus(UserStatus.ACTIVE);

        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            roleRepository.findByName(roleName).ifPresent(roles::add);
        }
        user.setRoles(roles);
        return userRepository.save(user);
    }

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}