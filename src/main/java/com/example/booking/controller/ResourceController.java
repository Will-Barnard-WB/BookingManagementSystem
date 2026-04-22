package com.example.booking.controller;

import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for resource CRUD operations.
 */
@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /** POST /resources — register a new bookable resource */
    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(
            @Valid @RequestBody CreateResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createResource(request));
    }

    /** GET /resources/{id} — retrieve a resource by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResource(@PathVariable UUID id) {
        return ResponseEntity.ok(resourceService.getResource(id));
    }

    /** GET /resources — list resources with pagination (?page=0&size=20&sort=createdAt,desc) */
    @GetMapping
    public ResponseEntity<Page<ResourceResponse>> getAllResources(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(resourceService.getAllResources(pageable));
    }

    /** PUT /resources/{id} — update resource details */
    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable UUID id,
            @Valid @RequestBody CreateResourceRequest request) {
        return ResponseEntity.ok(resourceService.updateResource(id, request));
    }

    /** DELETE /resources/{id} — remove a resource */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable UUID id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
