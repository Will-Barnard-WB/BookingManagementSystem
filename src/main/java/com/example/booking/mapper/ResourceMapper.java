package com.example.booking.mapper;

import com.example.booking.domain.entity.Resource;
import com.example.booking.dto.ResourceResponse;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for Resource entities.
 */
@Component
public class ResourceMapper {

    public ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .capacity(resource.getCapacity())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
