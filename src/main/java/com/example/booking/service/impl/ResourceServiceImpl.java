package com.example.booking.service.impl;

import com.example.booking.domain.entity.Resource;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.InvalidBookingException;
import com.example.booking.mapper.ResourceMapper;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.service.ResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;
    private final ResourceMapper resourceMapper;

    public ResourceServiceImpl(ResourceRepository resourceRepository,
                                BookingRepository bookingRepository,
                                ResourceMapper resourceMapper) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
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
    public Page<ResourceResponse> getAllResources(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(resourceMapper::toResponse);
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(UUID resourceId, CreateResourceRequest request) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Resource not found: " + resourceId));
        // Only reject the new name if it actually changed and clashes with another resource.
        if (!resource.getName().equals(request.getName())
                && resourceRepository.existsByName(request.getName())) {
            throw new InvalidBookingException(
                    "Resource name already exists: " + request.getName());
        }
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
        // Block deletion while the resource still has bookings that aren't cancelled,
        // otherwise those bookings would point at a resource that no longer exists.
        boolean hasActiveBookings = !bookingRepository
                .findByResourceIdAndStatusNot(resourceId, BookingStatus.CANCELLED)
                .isEmpty();
        if (hasActiveBookings) {
            throw new InvalidBookingException(
                    "Cannot delete resource with active bookings: " + resourceId);
        }
        resourceRepository.deleteById(resourceId);
    }
}
