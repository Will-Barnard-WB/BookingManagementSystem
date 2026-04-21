package com.example.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for POST /resources.
 */
public class CreateResourceRequest {

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 150, message = "name must be between 2 and 150 characters")
    private String name;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;

    @Min(value = 1, message = "capacity must be at least 1")
    private int capacity;

    // --- Constructors ---

    public CreateResourceRequest() {}

    public CreateResourceRequest(String name, String description, int capacity) {
        this.name = name;
        this.description = description;
        this.capacity = capacity;
    }

    // --- Getters & Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}
