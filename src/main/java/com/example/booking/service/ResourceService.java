package com.example.booking.service;

import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.ResourceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract for resource management.
 */
public interface ResourceService {

    ResourceResponse createResource(CreateResourceRequest request);

    ResourceResponse getResource(UUID resourceId);

    Page<ResourceResponse> getAllResources(Pageable pageable);

    ResourceResponse updateResource(UUID resourceId, CreateResourceRequest request);

    void deleteResource(UUID resourceId);
}
