package com.example.booking.repository;

import com.example.booking.domain.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Optional<Resource> findById(UUID id);


    List<Resource> findAll();

    boolean existsById(UUID id);

    boolean existsByName(String name);

    void deleteById(UUID id);
}
