package com.example.booking.repository;

import com.example.booking.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findById(UUID id);

    List<User> findAll();

    boolean existsById(UUID id);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    void deleteById(UUID id);
}
