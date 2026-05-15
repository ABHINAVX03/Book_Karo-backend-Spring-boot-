package com.codingshuttle.project.uber.uberApp.dto;

import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import lombok.Data;

@Data
public class OnboardDriverDto {
    private String vehicleId;
    private VehicleType vehicleType;
}
