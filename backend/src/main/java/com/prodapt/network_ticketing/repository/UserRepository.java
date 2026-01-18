package com.prodapt.network_ticketing.repository;

import com.prodapt.network_ticketing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByRole_RoleName(com.prodapt.network_ticketing.entity.enums.RoleName roleName);
}
