package com.example.booking.service;

import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.ResourceResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for resource management.
 */
public interface ResourceService {

    ResourceResponse createResource(CreateResourceRequest request);

    ResourceResponse getResource(UUID resourceId);

    List<ResourceResponse> getAllResources();

    ResourceResponse updateResource(UUID resourceId, CreateResourceRequest request);

    void deleteResource(UUID resourceId);
}
