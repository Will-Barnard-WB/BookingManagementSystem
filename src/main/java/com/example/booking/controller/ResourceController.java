package com.example.booking.controller;

import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    /** GET /resources — list all resources */
    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
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
