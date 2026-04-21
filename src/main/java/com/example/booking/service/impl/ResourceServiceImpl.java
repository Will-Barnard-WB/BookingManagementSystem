package com.example.booking.service.impl;

import com.example.booking.domain.entity.Resource;
import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.InvalidBookingException;
import com.example.booking.mapper.ResourceMapper;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.service.ResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    public ResourceServiceImpl(ResourceRepository resourceRepository,
                                ResourceMapper resourceMapper) {
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
    }

    @Override
    @Transactional
    public ResourceResponse createResource(CreateResourceRequest request) {
        if (resourceRepository.existsByName(request.getName())) {
            throw new InvalidBookingException(
                    "Resource name already exists: " + request.getName());
        }
        Resource resource = new Resource(
                request.getName(), request.getDescription(), request.getCapacity());
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Override
    public ResourceResponse getResource(UUID resourceId) {
        return resourceRepository.findById(resourceId)
                .map(resourceMapper::toResponse)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Resource not found: " + resourceId));
    }

    @Override
    public List<ResourceResponse> getAllResources() {
        return resourceRepository.findAll()
                .stream()
                .map(resourceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(UUID resourceId, CreateResourceRequest request) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Resource not found: " + resourceId));
        // TODO: check name uniqueness if name is being changed
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setCapacity(request.getCapacity());
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public void deleteResource(UUID resourceId) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new BookingNotFoundException("Resource not found: " + resourceId);
        }
        // TODO: check for active bookings on this resource before deleting
        resourceRepository.deleteById(resourceId);
    }
}
