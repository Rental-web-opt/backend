package com.rental.backend.repository;

import com.rental.backend.model.User;
import com.rental.backend.model.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    Boolean existsByEmail(String email);
}