package com.prodapt.network_ticketing.config;

import com.prodapt.network_ticketing.entity.Role;
import com.prodapt.network_ticketing.entity.User;
import com.prodapt.network_ticketing.entity.enums.RoleName;
import com.prodapt.network_ticketing.repository.RoleRepository;
import com.prodapt.network_ticketing.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Ensure roles exist
        Map<RoleName, Long> roleIds = Map.of(
            RoleName.CUSTOMER, 1L,
            RoleName.ENGINEER, 2L,
            RoleName.MANAGER, 3L,
            RoleName.ADMIN, 4L
        );

        for (Map.Entry<RoleName, Long> e : roleIds.entrySet()) {
            RoleName rn = e.getKey();
            Long id = e.getValue();
            roleRepository.findByRoleName(rn).orElseGet(() -> {
                Role r = new Role();
                r.setRoleId(id);
                r.setRoleName(rn);
                r.setDescription(rn.name() + " role");
                return roleRepository.save(r);
            });
        }

        // Sample users to create: managers, engineers, customers
        List<User> seeds = new ArrayList<>();

        // Managers
        seeds.add(buildUser("Alice Manager", "alice.manager@example.com", "manager.alice", "password123", RoleName.MANAGER));
        seeds.add(buildUser("Bob Manager", "bob.manager@example.com", "manager.bob", "password123", RoleName.MANAGER));

        // Engineers
        seeds.add(buildUser("Eve Engineer", "eve.engineer@example.com", "engineer.eve", "password123", RoleName.ENGINEER));
        seeds.add(buildUser("Sam Engineer", "sam.engineer@example.com", "engineer.sam", "password123", RoleName.ENGINEER));
        seeds.add(buildUser("Lina Engineer", "lina.engineer@example.com", "engineer.lina", "password123", RoleName.ENGINEER));

        // Customers
        seeds.add(buildUser("Charlie Customer", "charlie.customer@example.com", "customer.charlie", "password123", RoleName.CUSTOMER));
        seeds.add(buildUser("Dana Customer", "dana.customer@example.com", "customer.dana", "password123", RoleName.CUSTOMER));
        seeds.add(buildUser("Oscar Customer", "oscar.customer@example.com", "customer.oscar", "password123", RoleName.CUSTOMER));

        // Admin
        seeds.add(buildUser("Sys Admin", "admin@example.com", "admin", "admin123", RoleName.ADMIN));

        int created = 0;
        for (User u : seeds) {
            if (userRepository.findByUsername(u.getUsername()).isPresent()) continue;
            // attach role reference
            Role role = roleRepository.findByRoleName(u.getRole().getRoleName()).orElse(null);
            u.setRole(role);
            // encode password
            u.setPassword(passwordEncoder.encode(u.getPassword()));
            userRepository.save(u);
            created++;
        }

        if (created > 0) {
            System.out.println("Seeded " + created + " users (managers/engineers/customers).");
        }
    }

    private User buildUser(String name, String email, String username, String rawPassword, RoleName roleName) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setUsername(username);
        u.setPassword(rawPassword);
        Role r = new Role();
        r.setRoleName(roleName);
        u.setRole(r);
        u.setIsActive(true);
        return u;
    }
}
