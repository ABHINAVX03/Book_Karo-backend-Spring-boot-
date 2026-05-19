package com.codingshuttle.project.uber.uberApp.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PointDto {

    @NotNull(message = "Coordinates are required")
    private double[] coordinates;
    private String type = "Point";

    public PointDto(double[] coordinates) {
        this.coordinates = coordinates;
    }

    @AssertTrue(message = "Coordinates must be [longitude, latitude] within valid ranges")
    public boolean isValidCoordinates() {
        if (coordinates == null || coordinates.length != 2) return false;
        double longitude = coordinates[0];
        double latitude = coordinates[1];
        return Double.isFinite(longitude)
                && Double.isFinite(latitude)
                && longitude >= -180
                && longitude <= 180
                && latitude >= -90
                && latitude <= 90;
    }
}
